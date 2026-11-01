package com.errolmc.lightoverlay.client.overlay;

import com.errolmc.lightoverlay.client.config.DisplayMode;
import com.errolmc.lightoverlay.client.config.OverlayConfig;
import com.errolmc.lightoverlay.light.SpawnDanger;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Draws the marks as world geometry: crosses as lines, numbers as text lying flat on the block and turned to face the
 * player. Both are depth tested, so walls hide them, and drawn full bright so they show in the dark.
 */
public final class OverlayRenderer {
	private static final int RED = 0xFFFF3A3A;
	private static final int YELLOW = 0xFFFFD83A;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int WATER_BLUE = 0xFF5AC8FF;

	/** Half the size of a cross, in blocks. */
	private static final float CROSS_HALF_SIZE = 0.3F;
	private static final float LINE_WIDTH = 2.5F;
	/** World units per font pixel: digits come out about a third of a block tall. */
	private static final float TEXT_SCALE = 0.04F;
	/** Height above the surface, against z-fighting. */
	private static final float LIFT = 0.015F;
	/** Lines must not reach behind the camera, or the line shader smears them across the screen. */
	private static final float NEAR_PLANE_Z = -0.05F;

	private static final FormattedCharSequence[] LABELS = new FormattedCharSequence[16];

	static {
		for (int light = 0; light < LABELS.length; light++) {
			LABELS[light] = FormattedCharSequence.forward(Integer.toString(light), Style.EMPTY);
		}
	}

	private OverlayRenderer() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(OverlayRenderer::submit);
	}

	private static void submit(LevelRenderContext context) {
		List<Mark> marks = Overlay.marks();
		PoseStack poseStack = context.poseStack();
		CameraRenderState camera = context.levelState().cameraRenderState;
		if (marks.isEmpty() || poseStack == null || !camera.initialized) {
			return;
		}
		List<Mark> visible = marks.stream().filter(mark -> camera.cullFrustum.isVisible(bounds(mark))).toList();
		if (OverlayConfig.get().displayMode == DisplayMode.CROSSES) {
			submitCrosses(context.submitNodeCollector(), poseStack, camera, visible);
		} else {
			submitNumbers(context.submitNodeCollector(), poseStack, camera, visible);
		}
	}

	private static void submitCrosses(SubmitNodeCollector collector, PoseStack poseStack, CameraRenderState camera, List<Mark> marks) {
		// Marks scanned for the numbers mode include safe spots; crosses only go on the dangerous ones.
		List<Mark> crosses = marks.stream().filter(mark -> mark.danger() != SpawnDanger.SAFE).toList();
		if (crosses.isEmpty()) {
			return;
		}
		Vec3 cam = camera.pos;
		Matrix4f view = new Matrix4f(camera.viewRotationMatrix);
		collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
			Vector4f start = new Vector4f();
			Vector4f end = new Vector4f();
			for (Mark mark : crosses) {
				float x = (float) (mark.x() - cam.x);
				float y = (float) (mark.y() + LIFT - cam.y);
				float z = (float) (mark.z() - cam.z);
				int color = color(mark);
				start.set(x - CROSS_HALF_SIZE, y, z - CROSS_HALF_SIZE, 1);
				end.set(x + CROSS_HALF_SIZE, y, z + CROSS_HALF_SIZE, 1);
				line(buffer, pose, view, start, end, color);
				start.set(x - CROSS_HALF_SIZE, y, z + CROSS_HALF_SIZE, 1);
				end.set(x + CROSS_HALF_SIZE, y, z - CROSS_HALF_SIZE, 1);
				line(buffer, pose, view, start, end, color);
			}
		});
	}

	/** One line in camera-relative coordinates, cut where it passes behind the camera (as vanilla's gizmo lines do). */
	private static void line(VertexConsumer buffer, PoseStack.Pose pose, Matrix4fc view, Vector4f start, Vector4f end, int color) {
		float startZ = start.mul(view, new Vector4f()).z;
		float endZ = end.mul(view, new Vector4f()).z;
		boolean startBehind = startZ > NEAR_PLANE_Z;
		boolean endBehind = endZ > NEAR_PLANE_Z;
		if (startBehind && endBehind) {
			return;
		}
		if (startBehind || endBehind) {
			float cut = Mth.clamp((NEAR_PLANE_Z - startZ) / (endZ - startZ), 0.0F, 1.0F);
			Vector4f onPlane = start.lerp(end, cut, new Vector4f());
			(startBehind ? start : end).set(onPlane);
		}
		float dx = end.x - start.x;
		float dy = end.y - start.y;
		float dz = end.z - start.z;
		buffer.addVertex(pose, start.x, start.y, start.z).setNormal(pose, dx, dy, dz).setColor(color).setLineWidth(LINE_WIDTH);
		buffer.addVertex(pose, end.x, end.y, end.z).setNormal(pose, dx, dy, dz).setColor(color).setLineWidth(LINE_WIDTH);
	}

	private static void submitNumbers(SubmitNodeCollector collector, PoseStack poseStack, CameraRenderState camera, List<Mark> marks) {
		Font font = Minecraft.getInstance().font;
		Vec3 cam = camera.pos;
		// Flat on the surface, with the top of the digits pointing the way the camera faces, or north when fixed.
		float turn = OverlayConfig.get().numbersFaceYou ? 180.0F - camera.yRot : 0.0F;
		for (Mark mark : marks) {
			FormattedCharSequence label = LABELS[Mth.clamp(mark.light(), 0, 15)];
			poseStack.pushPose();
			poseStack.translate(mark.x() - cam.x, mark.y() + LIFT - cam.y, mark.z() - cam.z);
			poseStack.rotateDegrees(Axis.YP, turn);
			poseStack.rotateDegrees(Axis.XP, -90.0F);
			poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
			collector.submitText(poseStack, -font.width(label) / 2.0F, -font.lineHeight / 2.0F + 1, label, true,
				Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, numberColor(mark), 0, 0);
			poseStack.popPose();
		}
	}

	/** Like the crosses, except every 0 is red, even under the open sky or on blocks nothing spawns on. */
	private static int numberColor(Mark mark) {
		return mark.light() == 0 ? RED : color(mark);
	}

	private static int color(Mark mark) {
		return switch (mark.danger()) {
			case ALWAYS -> RED;
			case NIGHT -> YELLOW;
			case SAFE -> mark.water() ? WATER_BLUE : WHITE;
		};
	}

	private static AABB bounds(Mark mark) {
		return new AABB(mark.x() - 0.5, mark.y() - 0.1, mark.z() - 0.5, mark.x() + 0.5, mark.y() + 0.1, mark.z() + 0.5);
	}
}

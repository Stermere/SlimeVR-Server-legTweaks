package dev.slimevr.tracking.processor

import com.jme3.math.FastMath
import io.github.axisangles.ktmath.Quaternion
import io.github.axisangles.ktmath.Vector3


class TwistSwingConstraint(private val twist: Float, private val swing: Float) : Constraint()  {
	private fun decompose(rotation: Quaternion, twistAxis: Vector3): Pair<Quaternion, Quaternion> {
		val projection = rotation.project(twistAxis)

		val twist = Quaternion(rotation.w, projection.xyz).unit()
		val swing = rotation * twist.inv()

		return Pair(swing, twist)
	}

	private fun constrain(rotation: Quaternion, angle: Float): Quaternion {
		val length = FastMath.sin(Math.toRadians(angle.toDouble()).toFloat())
		val sqrLength = length * length
		var vector = Vector3(rotation.x, rotation.y, rotation.z)
		var rot = rotation

		if (vector.lenSq() > sqrLength) {
			vector = vector.unit() * length
			rot = Quaternion(FastMath.sqrt(1.0f - sqrLength) * FastMath.sign(rot.w),
				vector.x, vector.y, vector.z)
		}

		return rot
	}

	override fun applyConstraint(direction: Vector3, parent: Bone?): Quaternion {
		// if there is no parent or no constraint return the direction
		if (parent == null || (swing.isNaN() && twist.isNaN()))
			return Quaternion.fromTo(Vector3.NEG_Y, direction)

		// get the local rotation
		val rotationGlobal = Quaternion.fromTo(Vector3.NEG_Y, direction)
		val rotationLocal = parent.getGlobalRotation().inv() * rotationGlobal

		// decompose in to twist and swing
		var (swingQ, twistQ) = decompose(rotationLocal, Vector3.NEG_Y)

		// apply the constraints
		if (!swing.isNaN()) swingQ = constrain(swingQ, swing)
		if (!twist.isNaN()) twistQ = constrain(twistQ, twist)

		return parent.getGlobalRotation() * (swingQ * twistQ)
	}
}
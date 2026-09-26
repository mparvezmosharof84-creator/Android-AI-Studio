package com.parvez.aistudio

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Android AI Studio - High Performance 3D Engine Viewport (OpenGL ES 3.0)
 * Architect: Parvez Mosharof
 */
class Simple3DRenderer : GLSurfaceView.Renderer {

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)

    private var program = 0
    private var angle = 0f

    private val vertexShaderCode = """
        #version 300 es
        layout (location = 0) in vec4 vPosition;
        layout (location = 1) in vec4 aColor;
        uniform mat4 uMVPMatrix;
        out vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vColor = aColor;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #version 300 es
        precision mediump float;
        in vec4 vColor;
        out vec4 fragColor;
        void main() {
            fragColor = vColor;
        }
    """.trimIndent()

    private val cubeCoords = floatArrayOf(
        -0.5f,  0.5f,  0.5f,
        -0.5f, -0.5f,  0.5f,
         0.5f, -0.5f,  0.5f,
         0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,
         0.5f,  0.5f, -0.5f
    )

    private val cubeColors = floatArrayOf(
        0.16f, 0.78f, 1.0f, 1.0f,
        0.0f,  0.9f,  0.46f, 1.0f,
        1.0f,  0.34f, 0.2f, 1.0f,
        1.0f,  0.84f, 0.0f, 1.0f,
        0.6f,  0.2f,  0.8f, 1.0f,
        0.2f,  0.4f,  0.9f, 1.0f,
        0.9f,  0.2f,  0.5f, 1.0f,
        0.1f,  0.9f,  0.9f, 1.0f
    )

    private val drawOrder = shortArrayOf(
        0, 1, 2, 0, 2, 3,
        4, 5, 6, 4, 6, 7,
        0, 4, 7, 0, 7, 3,
        1, 5, 6, 1, 6, 2,
        3, 2, 6, 3, 6, 7,
        0, 1, 5, 0, 5, 4
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(cubeCoords.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(cubeCoords)
            position(0)
        }
    }

    private val colorBuffer: FloatBuffer = ByteBuffer.allocateDirect(cubeColors.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(cubeColors)
            position(0)
        }
    }

    private val drawListBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
            put(drawOrder)
            position(0)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.075f, 0.075f, 0.08f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 1.5f, -4f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setRotateM(rotationMatrix, 0, angle, 0.6f, 1.0f, 0.3f)
        val scratch = FloatArray(16)
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        GLES30.glUseProgram(program)

        val positionHandle = GLES30.glGetAttribLocation(program, "vPosition")
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 12, vertexBuffer)

        val colorHandle = GLES30.glGetAttribLocation(program, "aColor")
        GLES30.glEnableVertexAttribArray(colorHandle)
        GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, 16, colorBuffer)

        val mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix")
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, scratch, 0)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, drawOrder.size, GLES30.GL_UNSIGNED_SHORT, drawListBuffer)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(colorHandle)

        angle += 1.2f
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }
}

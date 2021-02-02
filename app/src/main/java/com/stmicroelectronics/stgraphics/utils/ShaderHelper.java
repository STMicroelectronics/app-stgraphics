package com.stmicroelectronics.stgraphics.utils;

import android.opengl.GLES20;

/**
 * Helper to create program associated to a predefined shader list
 */
public class ShaderHelper {

    /* Color only shader */

    private static final String VERTEX_SHADER_COLOR = "uniform mat4 u_MVPMatrix;                 \n" // A constant representing the combined model/view/projection matrix.
            + "attribute vec4 a_Position;     	     \n" // Per-vertex position information we will pass in.
            + "attribute vec4 a_Color;        	     \n" // Per-vertex color information we will pass in.

            + "varying vec4 v_Color;     	         \n" // This will be passed into the fragment shader.

            + "void main()      	                 \n" // The entry point for our vertex shader.
            + "{                              	     \n"
            + "   v_Color = a_Color;          	     \n" // Pass the color through to the fragment shader.
            + "   gl_Position = u_MVPMatrix   	     \n" // gl_Position is a special variable used to store the final position.
            + "               * a_Position;   	     \n" // Multiply the vertex by the matrix to get the final point
            + "}                              	     \n";

    private static final String VERTEX_SHADER_COLOR_LIGHT = "uniform mat4 u_MVPMatrix;           \n" // A constant representing the combined model/view/projection matrix.
            + "uniform mat4 u_MVMatrix;       	     \n" // A constant representing the combined model/view matrix.
            + "uniform vec3 u_LightPos;       	     \n" // The position of the light in eye space

            + "attribute vec4 a_Position;            \n" // Per-vertex position information we will pass in.
            + "attribute vec4 a_Color;               \n" // Per-vertex color information we will pass in.
            + "attribute vec3 a_Normal;       		 \n" // Per-vertex normal information we will pass in.

            + "varying vec4 v_Color;                 \n" // This will be passed into the fragment shader.

            + "void main()                           \n" // The entry point for our vertex shader.
            + "{                                     \n"
            + "  v_Color = vec4(0.0);                \n"
            + "  vec4 lightColor = vec4(1.0, 1.0, 0.85, 1.0);                                    \n" // Set warm light
            + "  vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);                           \n" // Transform the vertex into eye space.
            + "  vec3 modelViewNormal = normalize((u_MVMatrix * vec4(a_Normal, 0.0)).xyz);       \n" // Transform the normal's orientation into eye space.
            + "  vec3 lightVector = normalize(u_LightPos - modelViewVertex);                     \n" // Calculate the light vector.
            + "  float diffuse = max(dot(modelViewNormal, lightVector), 0.3);                    \n" // Calculate the dot product of the light vector and vertex normal.
            + "  v_Color += a_Color * lightColor * diffuse;                                      \n" // Apply the diffuse light.
            + "  v_Color += a_Color * lightColor * vec4(0.2, 0.2, 0.2, 1.0);                     \n" // Apply the ambient light.
            + "  vec3 eyeVector = normalize(vec3(0.0, 0.0, 1.0));                                \n" // Create eye vector (reversed).
            + "  vec3 lightReflectVector = reflect(vec3(0.0) - lightVector, modelViewNormal);    \n" // Calculate the light reflection vector.
            + "  float normalDotReflect = max(0.0, dot(eyeVector, lightReflectVector));          \n" // Calculate the dot product of the reflect light vector and the eye vector.
            + "  vec3 shininess = pow(normalDotReflect, 2.0) * vec3(1.0, 1.0, 1.0);              \n" // Calculate the shininess impact.
            + "  v_Color += a_Color * lightColor * vec4(shininess, 1.0);                         \n" // Apply the specular light.
            + "  clamp(v_Color, 0.0, 1.0);                                                       \n" // Make sure the color is between 0 and 1.
            + "  gl_Position = u_MVPMatrix           \n" // gl_Position is a special variable used to store the final position.
            + "               * a_Position;          \n" // Multiply the vertex by the matrix to get the final point
            + "}                                     \n";

    private static final String FRAGMENT_SHADER_COLOR = "precision mediump float;                \n" // Set the default precision to medium.
            + "varying vec4 v_Color;                 \n" // This is the color from the vertex shader interpolated across the
            + "void main()                           \n" // The entry point for our fragment shader.
            + "{                                     \n"
            + "  gl_FragColor = v_Color;             \n" // Pass the color directly through the pipeline.
            + "}                                     \n";

    /* Color and texture shader */

    private static final String VERTEX_SHADER_TEXTURE = "uniform mat4 u_MVPMatrix;               \n" // A constant representing the combined model/view/projection matrix.

            + "attribute vec4 a_Position;            \n" // Per-vertex position information we will pass in.
            + "attribute vec4 a_Color;               \n" // Per-vertex color information we will pass in.
            + "attribute vec2 a_TexCoordinate;       \n" // Per-vertex texture coordinate information we will pass in.

            + "varying vec4 v_Color;                 \n" // This will be passed into the fragment shader.
            + "varying vec2 v_TexCoordinate;         \n" // This will be passed into the fragment shader.

            + "void main()                           \n" // The entry point for our vertex shader.
            + "{                                     \n"
            + "  v_TexCoordinate = a_TexCoordinate;  \n" // Pass through the texture coordinate.
            + "  v_Color = a_Color;                  \n" // Pass the color through to the fragment shader.
            + "  gl_Position = u_MVPMatrix           \n" // gl_Position is a special variable used to store the final position.
            + "               * a_Position;          \n" // Multiply the vertex by the matrix to get the final point
            + "}                                     \n";

    private static final String FRAGMENT_SHADER_TEXTURE_OPT = "precision mediump float;          \n" // Set the default precision to medium.
            + "uniform sampler2D u_Texture;          \n" // The input texture
            + "uniform int u_TextState;              \n" // The texture state (texture enabled if > 0)

            + "varying vec4 v_Color;                 \n" // This input color
            + "varying vec2 v_TexCoordinate;         \n" // Interpolated texture coordinate per fragment

            + "void main()                           \n"
            + "{                                     \n"
            + "  if (u_TextState > 0) {              \n"
            + "    gl_FragColor = (v_Color * texture2D(u_Texture, v_TexCoordinate));             \n" // Multiply the color by the texture value to get final output color
            + "  } else {                            \n"
            + "    gl_FragColor = v_Color;           \n" // Multiply the color by the texture value to get final output color
            + "  }                                   \n"
            + "}                                     \n";

    // decision to not taken into account distance for the light attenuation
    private static final String VERTEX_SHADER_TEXTURE_LIGHT_OPT = "uniform mat4 u_MVPMatrix;     \n" // A constant representing the combined model/view/projection matrix.
            + "uniform mat4 u_MVMatrix;       	     \n" // A constant representing the combined model/view matrix.
            + "uniform vec3 u_LightPos;       	     \n" // The position of the light in eye space
            + "uniform int u_LightState;             \n" // The light state (light enabled if > 0)

            + "attribute vec4 a_Position;            \n" // Per-vertex position information we will pass in.
            + "attribute vec4 a_Color;               \n" // Per-vertex color information we will pass in.
            + "attribute vec2 a_TexCoordinate;       \n" // Per-vertex texture coordinate information we will pass in.
            + "attribute vec3 a_Normal;       		 \n" // Per-vertex normal information we will pass in.

            + "varying vec4 v_Color;                 \n" // This will be passed into the fragment shader.
            + "varying vec2 v_TexCoordinate;         \n" // This will be passed into the fragment shader.

            + "void main()                           \n" // The entry point for our vertex shader.
            + "{                                     \n"
            + "  if (u_LightState > 0) {             \n"
            + "    v_Color = vec4(0.0);              \n"
            + "    vec4 lightColor = vec4(1.0, 1.0, 0.85, 1.0);                                  \n" // Set warm light
            + "    vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);                         \n" // Transform the vertex into eye space.
            + "    vec3 modelViewNormal = normalize((u_MVMatrix * vec4(a_Normal, 0.0)).xyz);     \n" // Transform the normal's orientation into eye space.
            + "    vec3 lightVector = normalize(u_LightPos - modelViewVertex);                   \n" // Calculate the light vector.
            + "    float diffuse = max(dot(modelViewNormal, lightVector), 0.3);                  \n" // Calculate the dot product of the light vector and vertex normal.
            + "    v_Color += a_Color * lightColor * diffuse;                                    \n" // Apply the diffuse light.
            + "    v_Color += a_Color * lightColor * vec4(0.2, 0.2, 0.2, 1.0);                   \n" // Apply the ambient light.
            + "    vec3 eyeVector = normalize(vec3(0.0, 0.0, 1.0));                              \n" // Create eye vector (reversed).
            + "    vec3 lightReflectVector = reflect(vec3(0.0) - lightVector, modelViewNormal);  \n" // Calculate the light reflection vector.
            + "    float normalDotReflect = max(0.0, dot(eyeVector, lightReflectVector));        \n" // Calculate the dot product of the reflect light vector and the eye vector.
            + "    vec3 shininess = pow(normalDotReflect, 2.0) * vec3(1.0, 1.0, 1.0);            \n" // Calculate the shininess impact.
            + "    v_Color += a_Color * lightColor * vec4(shininess, 1.0);                       \n" // Apply the specular light.
            + "    clamp(v_Color, 0.0, 1.0);                                                     \n" // Make sure the color is between 0 and 1.
            + "  } else {                            \n"
            + "    v_Color = a_Color;                \n" // Pass the color through to the fragment shader.
            + "  }                                   \n"
            + "  v_TexCoordinate = a_TexCoordinate;  \n" // Pass through the texture coordinate.
            + "  gl_Position = u_MVPMatrix           \n" // gl_Position is a special variable used to store the final position.
            + "              * a_Position;           \n" // Multiply the vertex by the matrix to get the final point
            + "}                                     \n";


    private static final String FRAGMENT_SHADER_TEXTURE_OPT_CIRCLE =  "precision mediump float;  \n" // Set the default precision to medium
            + "uniform sampler2D u_Texture;          \n" // The input texture
            + "uniform int u_TextState;              \n" // The texture state (texture enabled if > 0)

            + "uniform vec2 u_CirclePosition;        \n" // The circle center position
            + "uniform float u_CircleRadius;         \n" // The circle radius

            + "varying vec4 v_Color;                 \n" // This input color
            + "varying vec2 v_TexCoordinate;         \n" // Interpolated texture coordinate per fragment

            + "void main()                           \n"
            + "{                                     \n"
            + "  float d, dist;                      \n"
            + "  dist = distance(u_CirclePosition, gl_FragCoord.xy);\n"
            + "  if(dist == 0.)                      \n"
            + "    dist = 1.;                        \n"
            + "  d = u_CircleRadius / dist;          \n"
            + "  if(d >= 1.) {                       \n"
            + "    if (u_TextState > 0) {            \n"
            + "      gl_FragColor = (v_Color * texture2D(u_Texture, v_TexCoordinate));           \n"
            + "    } else {                          \n"
            + "      gl_FragColor = v_Color;         \n"
            + "    }                                 \n"
            + "  } else {                            \n"
            + "    gl_FragColor = vec4(0.5, 0.5, 0.5, 0.5);                                      \n"
            + "  }                                   \n"
            + "}                                     \n";

    public static final int SHADER_COLOR = 0;
    public static final int SHADER_COLOR_LIGHT = 1;
    public static final int SHADER_TEXTURE = 2;
    public static final int SHADER_TEXTURE_LIGHT = 3;
    public static final int SHADER_TEXTURE_CIRCLE = 4;

    private static final String[] VERTEX_SHADER_TABLE = {
            VERTEX_SHADER_COLOR,                // SHADER_COLOR index
            VERTEX_SHADER_COLOR_LIGHT,          // SHADER_COLOR_LIGHT index
            VERTEX_SHADER_TEXTURE,              // SHADER_TEXTURE index
            VERTEX_SHADER_TEXTURE_LIGHT_OPT,    // SHADER_TEXTURE_LIGHT index
            VERTEX_SHADER_TEXTURE               // SHADER_TEXTURE_CIRCLE index
    };

    private static final String[] FRAGMENT_SHADER_TABLE = {
            FRAGMENT_SHADER_COLOR,              // SHADER_COLOR index
            FRAGMENT_SHADER_COLOR,              // SHADER_COLOR_LIGHT index
            FRAGMENT_SHADER_TEXTURE_OPT,        // SHADER_TEXTURE index
            FRAGMENT_SHADER_TEXTURE_OPT,        // SHADER_TEXTURE_LIGHT index
            FRAGMENT_SHADER_TEXTURE_OPT_CIRCLE  // SHADER_TEXTURE_CIRCLE index
    };

    /**
     * Compile the vertex shader received
     *
     * @param index kind of texture expected
     * @return vertex shader handle (null if the compilation failed)
     */
    public static int compileVertexShader(int index) {
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (vertexShaderHandle != 0)
        {
            GLES20.glShaderSource(vertexShaderHandle, VERTEX_SHADER_TABLE[index]);

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }
        return vertexShaderHandle;
    }

    /**
     * Compile the fragment shader received
     *
     * @param index kind of texture expected
     * @return fragment shader handle (null if the compilation failed)
     */
    public static int compileFragmentShader(int index) {
        // Load in the fragment shader.
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, FRAGMENT_SHADER_TABLE[index]);

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }
        return fragmentShaderHandle;
    }

    /**
     * Link the two vertex which shall be compiled previously
     * @param index kind of texture expected
     * @param vertexShaderHandle compiled vertex shader handle
     * @param fragmentShaderHandle compiled fragment shader handle
     * @return linked program handle (null if link failed)
     */
    public static int linkProgram(int index, int vertexShaderHandle, int fragmentShaderHandle) {
        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            // Bind the vertex shader to the program
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");

            if (index == SHADER_TEXTURE) {
                // case texture: bind the associated attribute
                GLES20.glBindAttribLocation(programHandle, 2, "a_TexCoordinate");
            }

            // Link the the vertex shader and fragment shader together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        return programHandle;
    }
}

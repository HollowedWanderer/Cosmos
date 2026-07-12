#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec4 vColor;
out vec2 vTexCoord;

void main() {
    float brightness = ColorModulator.x;
    float twinkleLow = ColorModulator.y;
    float twinkleHigh = ColorModulator.z;
    float time = ColorModulator.a;

    float twinkleSeed = fract(sin(dot(Position.xy, vec2(12.9898, 78.233))) * 43758.5453);
    float twinkleFrequency = mix(twinkleLow, twinkleHigh, twinkleSeed);

    float twinkle = 0.5 + 0.5 * sin(time * twinkleFrequency);
    if (Color.rgb == vec3(1.0, 1.0, 1.0)) {
        twinkle = 1.0;
    }

    vec3 modulated = Color.rgb * brightness * twinkle;
    if (Color.rgb == vec3(1.0, 1.0, 1.0)) {
        modulated *= 2;
    }

    vColor = vec4(modulated, Color.a);
    vTexCoord = UV0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
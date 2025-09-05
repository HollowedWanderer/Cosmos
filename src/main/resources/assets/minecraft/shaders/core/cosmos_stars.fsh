#version 150

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vColor;
in vec2 vTexCoord;
uniform sampler2D Sampler0;
out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, vTexCoord);
    fragColor = texColor * vColor;
}
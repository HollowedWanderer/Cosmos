#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec4 vColor;
layout(location = 1) in vec2 vTexCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, vTexCoord);
    fragColor = texColor * vColor;
}
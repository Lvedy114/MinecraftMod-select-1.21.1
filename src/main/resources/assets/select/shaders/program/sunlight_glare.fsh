#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Seconds;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

// 伪随机，用于打散采样避免出现条带
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 oneTexel = 1.0 / InSize;

    // 以屏幕中心为光源方向：越靠边缘散光越强，模拟瞳孔被阳光晃到后边缘发散
    vec2 toCenter = texCoord - vec2(0.5);
    float dist = length(toCenter);
    vec2 dir = dist > 0.0001 ? toCenter / dist : vec2(0.0);

    // 散光强度随时间正弦浮动，制造"晃眼"的不稳定感
    float flicker = 0.75 + 0.25 * sin(Seconds * 3.3) + 0.12 * sin(Seconds * 11.0);
    // 边缘权重：中心清晰，越往外散得越开（dist^1.5）
    float edge = pow(clamp(dist * 1.6, 0.0, 1.0), 1.5);
    float spread = Intensity * flicker * edge;

    // 沿径向方向做多次偏移采样（散光本质是点光源被拉成放射状条纹）
    const int SAMPLES = 12;
    float jitter = hash(texCoord * InSize) ;
    vec4 acc = vec4(0.0);
    float wsum = 0.0;
    for (int i = 0; i < SAMPLES; i++) {
        float t = (float(i) + jitter) / float(SAMPLES);
        // 沿径向向外拉伸采样，最大偏移随 spread 增大
        float off = (t - 0.5) * spread * 18.0;
        vec2 sampleUV = texCoord + dir * oneTexel * off;
        // 边缘略带垂直方向抖动，让散光更柔和不呈直线
        sampleUV += vec2(-dir.y, dir.x) * oneTexel * (t - 0.5) * spread * 6.0;
        float w = 1.0 - abs(t - 0.5) * 1.2;
        acc += texture(DiffuseSampler, sampleUV) * w;
        wsum += w;
    }
    vec4 blurred = acc / max(wsum, 0.0001);

    // 整体过曝提亮：阳光刺眼时画面发白
    float glow = Intensity * (0.18 + 0.10 * flicker) * (0.4 + 0.6 * edge);
    vec3 color = blurred.rgb + vec3(glow);

    // 中心叠加一团柔和白色光晕（直视阳光的核心眩光）
    float bloom = Intensity * 0.35 * exp(-dist * dist * 9.0) * flicker;
    color += vec3(bloom);

    fragColor = vec4(min(color, vec3(1.0)), 1.0);
}

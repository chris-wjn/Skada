#version 150

uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;
uniform vec2 InputSize;
uniform vec4 SourceEdgeFadeFlags;
uniform float ToneDebugMode;
uniform vec4 InkColor;
uniform vec4 PageColor;

in vec2 texCoord0;

out vec4 fragColor;

const float CLEAR_DEPTH = 0.99999;
const float TONE_COUNT = 6.0;
const float EDGE_FADE_WIDTH = 0.09;

float luminance(vec3 colour) {
  return dot(colour, vec3(0.2126, 0.7152, 0.0722));
}

float samplePreviewAlpha(vec2 uv) {
  return texture(Sampler0, uv).a;
}

float samplePreviewValueOr(vec2 uv, float fallbackValue) {
  if (samplePreviewAlpha(uv) <= 0.001) {
    return fallbackValue;
  }

  return luminance(texture(Sampler0, uv).rgb);
}

vec3 samplePreviewRgbOr(vec2 uv, vec3 fallbackColour) {
  if (samplePreviewAlpha(uv) <= 0.001) {
    return fallbackColour;
  }

  return texture(Sampler0, uv).rgb;
}

float sampleDepthOr(vec2 uv, float fallbackDepth) {
  if (samplePreviewAlpha(uv) <= 0.001) {
    return fallbackDepth;
  }

  float sampledDepth = texture(DepthSampler, uv).r;
  return sampledDepth >= CLEAR_DEPTH ? fallbackDepth : sampledDepth;
}

float samplePreviewDetail(vec2 uv, vec2 texel, float centerPreviewValue) {
  float leftPreview = samplePreviewValueOr(uv + vec2(-texel.x, 0.0), centerPreviewValue);
  float rightPreview = samplePreviewValueOr(uv + vec2(texel.x, 0.0), centerPreviewValue);
  float upPreview = samplePreviewValueOr(uv + vec2(0.0, -texel.y), centerPreviewValue);
  float downPreview = samplePreviewValueOr(uv + vec2(0.0, texel.y), centerPreviewValue);
  float neighbourhoodPreview = (leftPreview + rightPreview + upPreview + downPreview) * 0.25;
  return clamp(abs(centerPreviewValue - neighbourhoodPreview) * 1.65, 0.0, 1.0);
}

float colourChroma(vec3 colour) {
  float maxChannel = max(max(colour.r, colour.g), colour.b);
  float minChannel = min(min(colour.r, colour.g), colour.b);
  return maxChannel - minChannel;
}

float maxChannelValue(vec3 colour) {
  return max(max(colour.r, colour.g), colour.b);
}

float nearBlackEligibility(vec3 colour) {
  float maxChannel = maxChannelValue(colour);
  float chroma = colourChroma(colour);
  float luma = luminance(colour);
  float darknessMask = 1.0 - smoothstep(0.14, 0.30, maxChannel);
  float neutralMask = 1.0 - smoothstep(0.05, 0.16, chroma);
  float luminanceMask = 1.0 - smoothstep(0.06, 0.18, luma);
  return clamp(max(darknessMask * neutralMask, darknessMask * luminanceMask), 0.0, 1.0);
}

float samplePreviewChromaDetail(vec2 uv, vec2 texel, vec3 centerPreviewColour) {
  vec3 leftPreview = samplePreviewRgbOr(uv + vec2(-texel.x, 0.0), centerPreviewColour);
  vec3 rightPreview = samplePreviewRgbOr(uv + vec2(texel.x, 0.0), centerPreviewColour);
  vec3 upPreview = samplePreviewRgbOr(uv + vec2(0.0, -texel.y), centerPreviewColour);
  vec3 downPreview = samplePreviewRgbOr(uv + vec2(0.0, texel.y), centerPreviewColour);
  float centerChroma = colourChroma(centerPreviewColour);
  float neighbourhoodChroma = (colourChroma(leftPreview) + colourChroma(rightPreview)
      + colourChroma(upPreview) + colourChroma(downPreview)) * 0.25;
  float neighbourDistance = (distance(centerPreviewColour, leftPreview) + distance(centerPreviewColour, rightPreview)
      + distance(centerPreviewColour, upPreview) + distance(centerPreviewColour, downPreview)) * 0.25;
  float chromaActivity = max(centerChroma, neighbourhoodChroma);
  return smoothstep(0.03, 0.16, neighbourDistance + abs(centerChroma - neighbourhoodChroma) * 0.55)
      * smoothstep(0.04, 0.18, chromaActivity);
}

float samplePreviewToneField(vec2 uv, vec2 texel, float centerPreviewValue) {
  vec2 nearTexel = texel * 2.0;
  vec2 farTexel = texel * 5.0;
  float leftNear = samplePreviewValueOr(uv + vec2(-nearTexel.x, 0.0), centerPreviewValue);
  float rightNear = samplePreviewValueOr(uv + vec2(nearTexel.x, 0.0), centerPreviewValue);
  float upNear = samplePreviewValueOr(uv + vec2(0.0, -nearTexel.y), centerPreviewValue);
  float downNear = samplePreviewValueOr(uv + vec2(0.0, nearTexel.y), centerPreviewValue);
  float topLeftFar = samplePreviewValueOr(uv + vec2(-farTexel.x, -farTexel.y), centerPreviewValue);
  float topFar = samplePreviewValueOr(uv + vec2(0.0, -farTexel.y), centerPreviewValue);
  float topRightFar = samplePreviewValueOr(uv + vec2(farTexel.x, -farTexel.y), centerPreviewValue);
  float leftFar = samplePreviewValueOr(uv + vec2(-farTexel.x, 0.0), centerPreviewValue);
  float rightFar = samplePreviewValueOr(uv + vec2(farTexel.x, 0.0), centerPreviewValue);
  float bottomLeftFar = samplePreviewValueOr(uv + vec2(-farTexel.x, farTexel.y), centerPreviewValue);
  float bottomFar = samplePreviewValueOr(uv + vec2(0.0, farTexel.y), centerPreviewValue);
  float bottomRightFar = samplePreviewValueOr(uv + vec2(farTexel.x, farTexel.y), centerPreviewValue);
  float neighbourhood = centerPreviewValue * 4.0
      + (leftNear + rightNear + upNear + downNear) * 2.0
      + topLeftFar + topFar + topRightFar + leftFar + rightFar + bottomLeftFar + bottomFar + bottomRightFar;
  return neighbourhood / 20.0;
}

float samplePreviewLinework(vec2 uv, vec2 texel, float centerPreviewValue) {
  float topLeft = samplePreviewValueOr(uv + vec2(-texel.x, -texel.y), centerPreviewValue);
  float left = samplePreviewValueOr(uv + vec2(-texel.x, 0.0), centerPreviewValue);
  float bottomLeft = samplePreviewValueOr(uv + vec2(-texel.x, texel.y), centerPreviewValue);
  float top = samplePreviewValueOr(uv + vec2(0.0, -texel.y), centerPreviewValue);
  float bottom = samplePreviewValueOr(uv + vec2(0.0, texel.y), centerPreviewValue);
  float topRight = samplePreviewValueOr(uv + vec2(texel.x, -texel.y), centerPreviewValue);
  float right = samplePreviewValueOr(uv + vec2(texel.x, 0.0), centerPreviewValue);
  float bottomRight = samplePreviewValueOr(uv + vec2(texel.x, texel.y), centerPreviewValue);

  float topLeftFar = samplePreviewValueOr(uv + vec2(-texel.x * 2.0, -texel.y * 2.0), centerPreviewValue);
  float leftFar = samplePreviewValueOr(uv + vec2(-texel.x * 2.0, 0.0), centerPreviewValue);
  float bottomLeftFar = samplePreviewValueOr(uv + vec2(-texel.x * 2.0, texel.y * 2.0), centerPreviewValue);
  float topFar = samplePreviewValueOr(uv + vec2(0.0, -texel.y * 2.0), centerPreviewValue);
  float bottomFar = samplePreviewValueOr(uv + vec2(0.0, texel.y * 2.0), centerPreviewValue);
  float topRightFar = samplePreviewValueOr(uv + vec2(texel.x * 2.0, -texel.y * 2.0), centerPreviewValue);
  float rightFar = samplePreviewValueOr(uv + vec2(texel.x * 2.0, 0.0), centerPreviewValue);
  float bottomRightFar = samplePreviewValueOr(uv + vec2(texel.x * 2.0, texel.y * 2.0), centerPreviewValue);

  float sobelX = topLeft + 2.0 * left + bottomLeft - topRight - 2.0 * right - bottomRight;
  float sobelY = topLeft + 2.0 * top + topRight - bottomLeft - 2.0 * bottom - bottomRight;
  float farSobelX = topLeftFar + 2.0 * leftFar + bottomLeftFar - topRightFar - 2.0 * rightFar - bottomRightFar;
  float farSobelY = topLeftFar + 2.0 * topFar + topRightFar - bottomLeftFar - 2.0 * bottomFar - bottomRightFar;
  float edgeStrength = mix(length(vec2(sobelX, sobelY)), length(vec2(farSobelX, farSobelY)), 0.68);
  return smoothstep(0.22, 0.78, edgeStrength);
}

float sampleDepthEdge(vec2 uv, vec2 texel) {
  float centerDepth = sampleDepthOr(uv, texture(DepthSampler, uv).r);
  float leftDepth = sampleDepthOr(uv + vec2(-texel.x, 0.0), centerDepth);
  float rightDepth = sampleDepthOr(uv + vec2(texel.x, 0.0), centerDepth);
  float upDepth = sampleDepthOr(uv + vec2(0.0, -texel.y), centerDepth);
  float downDepth = sampleDepthOr(uv + vec2(0.0, texel.y), centerDepth);
  float gradient = length(vec2(rightDepth - leftDepth, downDepth - upDepth));
  return smoothstep(0.0006, 0.008, gradient);
}

float sampleAlphaEdge(vec2 uv, vec2 texel) {
  float centerAlpha = samplePreviewAlpha(uv);
  float leftAlpha = samplePreviewAlpha(uv + vec2(-texel.x, 0.0));
  float rightAlpha = samplePreviewAlpha(uv + vec2(texel.x, 0.0));
  float upAlpha = samplePreviewAlpha(uv + vec2(0.0, -texel.y));
  float downAlpha = samplePreviewAlpha(uv + vec2(0.0, texel.y));
  float alphaDelta = max(max(abs(centerAlpha - leftAlpha), abs(centerAlpha - rightAlpha)),
      max(abs(centerAlpha - upAlpha), abs(centerAlpha - downAlpha)));
  return smoothstep(0.02, 0.18, alphaDelta);
}

vec3 warmSepia(vec3 colour) {
  return clamp(colour * vec3(1.03, 0.99, 0.90) + vec3(0.012, 0.004, 0.0), 0.0, 1.0);
}

vec3 toneRampColour(float toneBand) {
  if (toneBand < 0.5) {
    return PageColor.rgb;
  }
  if (toneBand < 1.5) {
    return warmSepia(mix(PageColor.rgb, InkColor.rgb, 0.18));
  }
  if (toneBand < 2.5) {
    return warmSepia(mix(PageColor.rgb, InkColor.rgb, 0.34));
  }
  if (toneBand < 3.5) {
    return warmSepia(mix(PageColor.rgb, InkColor.rgb, 0.52));
  }
  if (toneBand < 4.5) {
    return warmSepia(mix(PageColor.rgb, InkColor.rgb, 0.72));
  }

  return warmSepia(mix(PageColor.rgb, InkColor.rgb, 0.94));
}

float toneRampAlpha(float toneBand) {
  if (toneBand < 0.5) {
    return 0.0;
  }
  if (toneBand < 1.5) {
    return 0.16;
  }
  if (toneBand < 2.5) {
    return 0.30;
  }
  if (toneBand < 3.5) {
    return 0.48;
  }
  if (toneBand < 4.5) {
    return 0.68;
  }

  return 0.92;
}

float sourceEdgeFadeMask(vec2 uv) {
  float sourceMask = 1.0;
  if (SourceEdgeFadeFlags.x > 0.5) {
    sourceMask *= smoothstep(0.0, EDGE_FADE_WIDTH, uv.x);
  }
  if (SourceEdgeFadeFlags.y > 0.5) {
    sourceMask *= smoothstep(0.0, EDGE_FADE_WIDTH, uv.y);
  }
  if (SourceEdgeFadeFlags.z > 0.5) {
    sourceMask *= 1.0 - smoothstep(1.0 - EDGE_FADE_WIDTH, 1.0, uv.x);
  }
  if (SourceEdgeFadeFlags.w > 0.5) {
    sourceMask *= 1.0 - smoothstep(1.0 - EDGE_FADE_WIDTH, 1.0, uv.y);
  }
  return sourceMask;
}

void main() {
  vec2 sourceUv = texCoord0;
  vec4 previewSample = texture(Sampler0, sourceUv);
  if (previewSample.a <= 0.001) {
    discard;
  }

  vec2 texel = 1.0 / max(InputSize, vec2(1.0));
  float gray = luminance(previewSample.rgb);
  float toneGray = samplePreviewToneField(sourceUv, texel, gray);
  float detail = samplePreviewDetail(sourceUv, texel, gray);
  float chromaDetail = samplePreviewChromaDetail(sourceUv, texel, previewSample.rgb);
  float previewLinework = samplePreviewLinework(sourceUv, texel, gray);
  float depthEdge = sampleDepthEdge(sourceUv, texel);
  float alphaEdge = sampleAlphaEdge(sourceUv, texel);
  float contourMask = max(max(alphaEdge, depthEdge * 0.98), max(chromaDetail * 0.58, previewLinework * 0.62));
  float structureShadow = previewLinework * 0.12 + detail * 0.06 + depthEdge * 0.08 + chromaDetail * 0.04;
  float posterSource = clamp(toneGray - structureShadow, 0.0, 1.0);
  float rawDarkness = clamp((1.0 - posterSource) + contourMask * 0.24 + previewLinework * 0.06
      + chromaDetail * 0.03, 0.0, 0.9999);
  float chromaLift = smoothstep(0.10, 0.28, colourChroma(previewSample.rgb))
      * smoothstep(0.58, 0.92, rawDarkness) * 0.12;
  float darkness = clamp(rawDarkness - chromaLift, 0.0, 0.9999);
  float darkestToneGate = step(0.72, nearBlackEligibility(previewSample.rgb) * smoothstep(0.84, 0.98, darkness));
  float toneBand = floor(darkness * (TONE_COUNT - 1.0)) + darkestToneGate;
  toneBand = clamp(toneBand, 0.0, TONE_COUNT - 1.0);

  if (ToneDebugMode > 0.5) {
    if (ToneDebugMode >= 3.5) {
      float quantizedGray = 1.0 - toneBand / (TONE_COUNT - 1.0);
      fragColor = vec4(vec3(quantizedGray), previewSample.a);
      return;
    }
    if (ToneDebugMode >= 2.5) {
      fragColor = vec4(vec3(toneGray), previewSample.a);
      return;
    }
    fragColor = vec4(vec3(gray), previewSample.a);
    return;
  }

  vec3 sepiaTone = toneRampColour(toneBand);
  float contourInk = smoothstep(0.04, 0.18, contourMask) * 0.28;
  sepiaTone = mix(sepiaTone, InkColor.rgb, contourInk);
  float toneAlpha = toneRampAlpha(toneBand) + contourInk * 0.12;
  fragColor = vec4(sepiaTone, clamp(toneAlpha * previewSample.a * sourceEdgeFadeMask(texCoord0), 0.0, 1.0));
}
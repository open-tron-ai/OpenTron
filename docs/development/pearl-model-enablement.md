# Pearl Model Enablement

This page tracks the work required to make a new Hugging Face model mineable
through Pearl's vLLM miner and OpenTron.

OpenTron can point `vllm-pearl` at a model id, but a raw Hugging Face model is
not enough. The Pearl vLLM plugin expects a Pearl-compatible quantized model
whose metadata marks mining layers for 7-bit NoisyGEMM and non-mining layers
for the vanilla Pearl GEMM path.

## Supported Models

OpenTron only supports Pearl models published by the `pearl-ai` Hugging Face
organization. Private staging artifacts and OpenTron-specific conversion
repos are not user-facing supported mining models.

The current public support set is:

| Raw model | Pearl model | Status |
|---|---|---|
| `meta-llama/Llama-3.3-70B-Instruct` | `pearl-ai/Llama-3.3-70B-Instruct-pearl` | Validated default |
| `google/gemma-4-31B-it` | `pearl-ai/Gemma-4-31B-it-pearl` | Public validation candidate; not yet promoted to default |
| `meta-llama/Llama-3.1-8B-Instruct` | `pearl-ai/Llama-3.1-8B-Instruct-pearl` | Public validation candidate; not yet promoted to default |

## Current Validation Findings

The current implementation has a validated default path for the Llama Pearl
model on NVIDIA H100/H200 hosts. That path is exercised end to end through
`Tron mine start`, vLLM `/v1/models`, OpenTron inference routing, Pearl
gateway template refresh, and `Tron mine validate-model`.

`pearl-ai/Gemma-4-31B-it-pearl` and
`pearl-ai/Llama-3.1-8B-Instruct-pearl` remain visible as public validation
candidates. They are not promoted to the default supported set until the
relevant H100/H200 validation artifacts are recorded and attached to the model
enablement flow.

## Enablement Checklist

1. Reproduce the current Llama Pearl model recipe.
   - Record the compressed-tensors config.
   - Record which linear layers are 7-bit mining layers.
   - Record which layers are 8-bit non-mining layers.
   - Record calibration data and SmoothQuant settings, if used.

2. Convert the target model.
   - Start with a model Pearl intends to publish under the `pearl-ai` org.
   - Generate Pearl-compatible quantized weights and metadata.
   - For Gemma4 artifacts, include the base model's processor metadata required
     by vLLM's Gemma4 multimodal profiler.
   - Publish under the planned `pearl-ai/*-pearl` id before enabling it in
     OpenTron.

   OpenTron includes an experimental local converter for this work:

   ```bash
   python scripts/pearl/model_converter.py \
     meta-llama/Llama-3.1-8B-Instruct \
     /tmp/pearl-ai-Llama-3.1-8B-Instruct-pearl \
     --device cuda
   ```

   The converter copies Hugging Face metadata, emits
   `quantization_config.quant_method = "pearl"`, writes a safetensors index,
   converts attention q/k/v and MLP down projections to int8 non-mining layers,
   and converts the remaining text linear weights to int7 mining layers. Treat
   its output as a staging artifact until `Tron mine inspect-model` and
   `Tron mine validate-model` pass on H100/H200 hardware.

   Local staging artifacts can be inspected before upload:

   ```bash
   Tron mine inspect-model \
     --model /tmp/pearl-ai-Llama-3.1-8B-Instruct-pearl
   ```

   To run a local staging artifact through the Docker miner, keep `--model` as
   the intended served model name and point `--local-model-path` at the
   converted checkpoint directory:

   ```bash
   Tron mine init \
     --provider vllm-pearl \
     --wallet-address <prl1...> \
     --model pearl-ai/Llama-3.1-8B-Instruct-pearl \
     --local-model-path /tmp/pearl-ai-Llama-3.1-8B-Instruct-pearl \
     --cuda-visible-devices 1 \
     --vllm-arg=--language-model-only \
     --vllm-arg=--skip-mm-profiling
   Tron mine start
   ```

3. Validate the Pearl vLLM plugin path.
   - Run `Tron mine inspect-model --model <pearl-model-id>
     --allow-planned` before starting the miner.
   - Model loads in Pearl's `vllm-miner` container.
   - vLLM registers Pearl's quantization plugin.
   - Mining layers use int7 NoisyGEMM.
   - Non-mining layers use int8 vanilla Pearl GEMM.
   - Text generation works with mining enabled and disabled.

4. Validate chain integration.
   - `pearld` is reachable.
   - `pearl-gateway` receives work.
   - NoisyGEMM submits candidate proofs.
   - Gateway reports metrics.
   - `Tron mine status` parses those metrics.

5. Promote the model in OpenTron.
   - Change its registry status from `planned` to `validated`.
   - Set measured VRAM and context defaults.
   - Add the model to user docs.
   - Attach validation logs to the PR.

## OpenTron Registry

Model support metadata lives in:

```text
src/OpenTron/mining/_models.py
```

`Tron mine models` renders that registry. Planned models are visible to users
but blocked by capability detection until the Pearl model artifact and H100/H200
validation exist.

## Acceptance Criteria

A model is `validated` only when all of these pass on real hardware:

- `Tron mine inspect-model --model <pearl-model-id> --allow-planned`
- `Tron mine init --model <pearl-model-id>`
- `Tron mine start`
- `curl http://127.0.0.1:8000/v1/models`
- `Tron ask "Say hello in one sentence."`
- `Tron mine status`
- `Tron mine validate-model --model <pearl-model-id> --allow-planned --prompt
  "Say hello in one sentence." --output <artifact>.json`
- Pearl gateway metrics show the mining path is active.
- No block/share submission errors appear in gateway or miner logs.

Do not mark a model validated based only on vLLM load success. It must exercise
Pearl's NoisyGEMM and submission path.

## Tracking

Use the `Pearl Model Validation` GitHub issue template for each candidate model.
The issue should hold the quantization recipe, hardware details, command output,
metrics excerpts, and the PR that changes the model status to `validated`.
Attach the JSON artifact from `Tron mine validate-model --output` to the issue.


<script setup lang="ts">
import { useAppShellStore } from '~/stores/app-shell'

const appShell = useAppShellStore()

const futureAreas = [
  'Merchant dashboard',
  'Admin dashboard',
  'Payment operations',
  'Risk and review',
  'Reconciliation'
]
</script>

<template>
  <main class="min-h-screen bg-slate-950 text-white">
    <section class="mx-auto flex min-h-screen w-full max-w-6xl flex-col px-6 py-8 lg:px-8">
      <header class="flex items-center justify-between border-b border-white/10 pb-6">
        <div>
          <p class="text-sm font-medium uppercase tracking-[0.32em] text-primary-300">
            Phase 0 foundation
          </p>
          <h1 class="mt-3 text-3xl font-semibold tracking-tight sm:text-5xl">
            Payment Quality Engineering Lab
          </h1>
        </div>

        <UBadge color="primary" variant="soft" size="lg">
          {{ appShell.phaseLabel }}
        </UBadge>
      </header>

      <div class="grid flex-1 gap-6 py-10 lg:grid-cols-[1.1fr_0.9fr]">
        <UCard class="bg-white/5 ring-white/10">
          <template #header>
            <div>
              <h2 class="text-xl font-semibold text-white">
                Running skeleton, not payment functionality
              </h2>
              <p class="mt-2 text-sm text-slate-300">
                This shell proves that Nuxt, Nuxt UI, TypeScript, Zod, Pinia, and Playwright have a coherent place to grow.
              </p>
            </div>
          </template>

          <p class="text-slate-300">
            Merchant, admin, payment operations, risk/review, and reconciliation dashboards are intentionally deferred to later phases.
          </p>

          <div class="mt-6 flex flex-wrap gap-3">
            <UBadge v-for="area in futureAreas" :key="area" variant="subtle" color="neutral">
              {{ area }}
            </UBadge>
          </div>
        </UCard>

        <UCard class="bg-white/5 ring-white/10">
          <template #header>
            <h2 class="text-xl font-semibold text-white">
              Tester orientation
            </h2>
          </template>

          <ul class="space-y-4 text-sm text-slate-300">
            <li>Verify setup and commands from a clean contributor perspective.</li>
            <li>Confirm no payment workflow, PSP flow, Kafka setup, or full auth flow exists.</li>
            <li>Review test conventions for future order-independent and worker-aware automation.</li>
          </ul>
        </UCard>
      </div>
    </section>
  </main>
</template>

import { test, expect } from '../fixtures'

const tiles = [
  'visual-tile-merchant-badge',
  'visual-tile-payment-badge',
  'visual-tile-problem-details',
  'visual-tile-hosted-cta',
  'visual-tile-idle-lock',
  'visual-tile-dark',
  'visual-tile-expired',
] as const

test.describe('Visual Lab tiles on live --app', () => {
  test.beforeEach(async ({ app }) => {
    await app.visualLab.goto()
    await app.visualLab.expectLoaded()
  })

  for (const tile of tiles) {
    test(`screenshot ${tile}`, { tag: ['@visual'] }, async ({ app }) => {
      await expect(app.visualLab.tile(tile)).toHaveScreenshot(`${tile}.png`, {
        stylePath: 'tests-pom/visual-lab-mask.css',
      })
    })
  }

  test('break visual is tagged and not default CI', { tag: ['@visual', '@visual-negative'] }, async ({ app }) => {
    await app.visualLab.toggleBreak()
    await expect(app.visualLab.tile('visual-tile-hosted-cta')).toHaveScreenshot('visual-tile-hosted-cta-broken.png', {
      stylePath: 'tests-pom/visual-lab-mask.css',
    })
  })
})

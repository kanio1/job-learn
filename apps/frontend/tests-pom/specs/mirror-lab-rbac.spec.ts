import { expect, test } from '../fixtures'
test('maker cannot self-approve; checker can', async ({ actors }) => {
  const manager = await actors.open('merchantManager')
  const admin = await actors.open('platformAdmin')
    await manager.app.mirrorBank.goto()
    await manager.app.mirrorBank.expectLoaded()
    await manager.app.mirrorBank.createApproval()
    await expect(manager.app.mirrorBank.approvalIdInput()).not.toHaveValue('')
    const approvalId = (await manager.app.mirrorBank.approvalIdInput().inputValue()).trim()
    expect(approvalId.length).toBeGreaterThan(8)

    await manager.app.mirrorBank.approveApproval()
    await manager.app.mirrorBank.confirmApproval()
    await expect(manager.app.mirrorBank.approvalResult()).toContainText('403')

    await admin.app.mirrorBank.goto()
    await admin.app.mirrorBank.expectLoaded()
    await admin.app.mirrorBank.approvalIdInput().fill(approvalId)
    await admin.app.mirrorBank.approveApproval()
    await admin.app.mirrorBank.confirmApproval()
    await expect(admin.app.mirrorBank.approvalResult()).toContainText('200')
})

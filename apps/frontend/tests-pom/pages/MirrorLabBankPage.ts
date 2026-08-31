import { expect, type Download, type Locator } from '@playwright/test'
import { BasePage } from './BasePage'

export class MirrorLabBankPage extends BasePage {
  override async goto(): Promise<void> {
    await super.goto('/admin/mirror-lab/bank')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.byTestId('step-up-submit')).toBeVisible()
  }

  async downloadCsv(): Promise<Download> {
    return this.downloadByTestId('statement-download-csv')
  }

  async downloadPdf(): Promise<Download> {
    return this.downloadByTestId('statement-download-pdf')
  }

  approvalIdInput(): Locator {
    return this.byTestId('approval-id')
  }

  approvalResult(): Locator { return this.byTestId('approval-result') }

  async createApproval(): Promise<void> {
    await this.byTestId('approval-create').click()
  }

  async approveApproval(): Promise<void> {
    await this.byTestId('approval-approve').click()
  }

  async confirmApproval(): Promise<void> {
    await this.byTestId('confirm-action-confirm').click()
  }

  private async downloadByTestId(testId: string): Promise<Download> {
    const download = this.page.waitForEvent('download')
    await this.byTestId(testId).click()
    return download
  }
}

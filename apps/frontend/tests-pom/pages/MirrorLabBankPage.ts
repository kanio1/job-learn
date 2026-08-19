import { expect, type Download } from '@playwright/test'
import { BasePage } from './BasePage'

export class MirrorLabBankPage extends BasePage {
  async goto(): Promise<void> {
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

  private async downloadByTestId(testId: string): Promise<Download> {
    const download = this.page.waitForEvent('download')
    await this.byTestId(testId).click()
    return download
  }
}

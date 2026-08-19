/** Builder for operator payment-order bodies. Unique reference stays in factories. */
export class PaymentOrderDraft {
  private amountMinor = 1999
  private currency = 'PLN'
  private clientOrderReference = 'PO-DRAFT'

  static builder(): PaymentOrderDraft {
    return new PaymentOrderDraft()
  }

  amount(amountMinor: number): this {
    this.amountMinor = amountMinor
    return this
  }

  pln(): this {
    this.currency = 'PLN'
    return this
  }

  eur(): this {
    this.currency = 'EUR'
    return this
  }

  reference(clientOrderReference: string): this {
    this.clientOrderReference = clientOrderReference
    return this
  }

  build(): { amountMinor: number, currency: string, clientOrderReference: string } {
    return {
      amountMinor: this.amountMinor,
      currency: this.currency,
      clientOrderReference: this.clientOrderReference,
    }
  }
}

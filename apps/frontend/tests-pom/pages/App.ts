import type { Page } from '@playwright/test'
import { Sidebar } from './components/Sidebar'
import { ProblemDetailsCard } from './components/ProblemDetailsCard'
import { UserMenu } from './components/UserMenu'
import { CommandPalette } from './components/CommandPalette'
import { LoginPage } from './LoginPage'
import { MerchantsListPage } from './MerchantsListPage'
import { MerchantDetailPage } from './MerchantDetailPage'
import { PaymentsListPage } from './PaymentsListPage'
import { PaymentCreatePage } from './PaymentCreatePage'
import { PaymentDetailPage } from './PaymentDetailPage'
import { UsersPage } from './UsersPage'
import { AuditPage } from './AuditPage'
import { TenantSettingsPage } from './TenantSettingsPage'
import { ErrorLabPage } from './ErrorLabPage'
import { CheckoutLabHubPage } from './CheckoutLabHubPage'
import { CheckoutLabBookingPage } from './CheckoutLabBookingPage'
import { CheckoutLabInspectorPage } from './CheckoutLabInspectorPage'
import { HostedCheckoutPage } from './HostedCheckoutPage'
import { CheckoutReturnPage } from './CheckoutReturnPage'
import { SupportPage } from './SupportPage'
import { MirrorLabHubPage } from './MirrorLabHubPage'
import { SessionLabPage } from './SessionLabPage'
import { NetworkLabPage } from './NetworkLabPage'
import { MirrorLabBankPage } from './MirrorLabBankPage'

/** Facade: one fixture exposes every page object for the current Page. */
export class App {
  readonly sidebar: Sidebar
  readonly problem: ProblemDetailsCard
  readonly userMenu: UserMenu
  readonly commandPalette: CommandPalette
  readonly login: LoginPage
  readonly merchants: MerchantsListPage
  readonly merchantDetail: MerchantDetailPage
  readonly payments: PaymentsListPage
  readonly paymentCreate: PaymentCreatePage
  readonly paymentDetail: PaymentDetailPage
  readonly users: UsersPage
  readonly audit: AuditPage
  readonly tenantSettings: TenantSettingsPage
  readonly errorLab: ErrorLabPage
  readonly checkoutHub: CheckoutLabHubPage
  readonly checkoutBooking: CheckoutLabBookingPage
  readonly checkoutInspector: CheckoutLabInspectorPage
  readonly hostedCheckout: HostedCheckoutPage
  readonly checkoutReturn: CheckoutReturnPage
  readonly support: SupportPage
  readonly mirrorHub: MirrorLabHubPage
  readonly sessionLab: SessionLabPage
  readonly networkLab: NetworkLabPage
  readonly mirrorBank: MirrorLabBankPage

  constructor(readonly page: Page) {
    this.sidebar = new Sidebar(page)
    this.problem = new ProblemDetailsCard(page)
    this.userMenu = new UserMenu(page)
    this.commandPalette = new CommandPalette(page)
    this.login = new LoginPage(page)
    this.merchants = new MerchantsListPage(page)
    this.merchantDetail = new MerchantDetailPage(page)
    this.payments = new PaymentsListPage(page)
    this.paymentCreate = new PaymentCreatePage(page)
    this.paymentDetail = new PaymentDetailPage(page)
    this.users = new UsersPage(page)
    this.audit = new AuditPage(page)
    this.tenantSettings = new TenantSettingsPage(page)
    this.errorLab = new ErrorLabPage(page)
    this.checkoutHub = new CheckoutLabHubPage(page)
    this.checkoutBooking = new CheckoutLabBookingPage(page)
    this.checkoutInspector = new CheckoutLabInspectorPage(page)
    this.hostedCheckout = new HostedCheckoutPage(page)
    this.checkoutReturn = new CheckoutReturnPage(page)
    this.support = new SupportPage(page)
    this.mirrorHub = new MirrorLabHubPage(page)
    this.sessionLab = new SessionLabPage(page)
    this.networkLab = new NetworkLabPage(page)
    this.mirrorBank = new MirrorLabBankPage(page)
  }
}

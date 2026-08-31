import type { Page } from '@playwright/test'
import { Sidebar } from './components/Sidebar'
import { ProblemDetailsCard } from './components/ProblemDetailsCard'
import { UserMenu } from './components/UserMenu'
import { CommandPalette } from './components/CommandPalette'
import { LocaleSelectComponent } from './components/LocaleSelectComponent'
import { IdleOverlay } from './components/IdleOverlay'
import { LoginPage } from './LoginPage'
import { MerchantsListPage } from './MerchantsListPage'
import { MerchantDetailPage } from './MerchantDetailPage'
import { MerchantSlideover } from './components/MerchantSlideover'
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
import { CheckoutLabWidgetPage } from './CheckoutLabWidgetPage'
import { HostedCheckoutPage } from './HostedCheckoutPage'
import { CheckoutReturnPage } from './CheckoutReturnPage'
import { SupportPage } from './SupportPage'
import { OverviewPage } from './OverviewPage'
import { MirrorLabHubPage } from './MirrorLabHubPage'
import { SessionLabPage } from './SessionLabPage'
import { NetworkLabPage } from './NetworkLabPage'
import { MirrorLabBankPage } from './MirrorLabBankPage'
import { RlsLabPage } from './RlsLabPage'
import { VisualLabPage } from './VisualLabPage'
import { PspRedirectSimulatorPage } from './PspRedirectSimulatorPage'
import { EventLabPage } from './EventLabPage'
import { ForbiddenPage } from './ForbiddenPage'

/** Facade: one fixture exposes every page object for the current Page. */
export class App {
  readonly sidebar: Sidebar
  readonly problem: ProblemDetailsCard
  readonly userMenu: UserMenu
  readonly commandPalette: CommandPalette
  readonly localeSelect: LocaleSelectComponent
  readonly idle: IdleOverlay
  readonly login: LoginPage
  readonly merchants: MerchantsListPage
  readonly merchantDetail: MerchantDetailPage
  readonly merchantSlideover: MerchantSlideover
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
  readonly checkoutWidget: CheckoutLabWidgetPage
  readonly hostedCheckout: HostedCheckoutPage
  readonly checkoutReturn: CheckoutReturnPage
  readonly support: SupportPage
  readonly overview: OverviewPage
  readonly mirrorHub: MirrorLabHubPage
  readonly sessionLab: SessionLabPage
  readonly networkLab: NetworkLabPage
  readonly mirrorBank: MirrorLabBankPage
  readonly rlsLab: RlsLabPage
  readonly visualLab: VisualLabPage
  readonly pspSimulator: PspRedirectSimulatorPage
  readonly eventLab: EventLabPage
  readonly forbidden: ForbiddenPage

  constructor(readonly page: Page) {
    this.sidebar = new Sidebar(page)
    this.problem = new ProblemDetailsCard(page)
    this.userMenu = new UserMenu(page)
    this.commandPalette = new CommandPalette(page)
    this.localeSelect = new LocaleSelectComponent(page)
    this.idle = new IdleOverlay(page)
    this.login = new LoginPage(page)
    this.merchants = new MerchantsListPage(page)
    this.merchantDetail = new MerchantDetailPage(page)
    this.merchantSlideover = new MerchantSlideover(page)
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
    this.checkoutWidget = new CheckoutLabWidgetPage(page)
    this.hostedCheckout = new HostedCheckoutPage(page)
    this.checkoutReturn = new CheckoutReturnPage(page)
    this.support = new SupportPage(page)
    this.overview = new OverviewPage(page)
    this.mirrorHub = new MirrorLabHubPage(page)
    this.sessionLab = new SessionLabPage(page)
    this.networkLab = new NetworkLabPage(page)
    this.mirrorBank = new MirrorLabBankPage(page)
    this.rlsLab = new RlsLabPage(page)
    this.visualLab = new VisualLabPage(page)
    this.pspSimulator = new PspRedirectSimulatorPage(page)
    this.eventLab = new EventLabPage(page)
    this.forbidden = new ForbiddenPage(page)
  }
}

import { z } from 'zod'
import {
  anomalySchema,
  bookingResultSchema,
  checkoutEventSchema,
  createSessionResponseSchema,
  deliverySchema,
  fulfillmentSchema,
  hostedCheckoutSessionSchema,
  type BookingResult,
} from '~/schemas/checkout-lab.schema'

export function useCheckoutLabApi() {
  const { request } = useApiClient()

  function createSession(body: Record<string, unknown>, headers?: Record<string, string>) {
    return request('/api/checkout-lab/sessions', createSessionResponseSchema, {
      method: 'POST',
      body,
      headers,
      redirect: 'manual',
    })
  }

  function createBooking(body: Record<string, unknown>, headers?: Record<string, string>) {
    return request('/api/checkout-lab/bookings', bookingResultSchema, {
      method: 'POST',
      body,
      headers,
    })
  }

  function getHostedSession(sessionId: string) {
    return request(`/api/checkout-lab/hosted/sessions/${sessionId}`, hostedCheckoutSessionSchema)
  }

  function simulate(sessionId: string, outcome: string, simulateToken?: string | null) {
    const headers: Record<string, string> = {}
    if (simulateToken) {
      headers['Lab-Simulate-Token'] = simulateToken
    }
    return request(`/api/checkout-lab/hosted/sessions/${sessionId}/simulate`, hostedCheckoutSessionSchema, {
      method: 'POST',
      body: { outcome },
      headers,
    })
  }

  function getHostedFulfillment(sessionId: string) {
    return request(`/api/checkout-lab/hosted/sessions/${sessionId}/fulfillment`, fulfillmentSchema)
  }

  function listEvents(sessionId: string) {
    return request(`/api/checkout-lab/sessions/${sessionId}/events`, z.array(checkoutEventSchema))
  }

  function listDeliveries(sessionId: string) {
    return request(`/api/checkout-lab/sessions/${sessionId}/deliveries`, z.array(deliverySchema))
  }

  function listAnomalies() {
    return request('/api/checkout-lab/anomalies', z.array(anomalySchema))
  }

  function getBooking(bookingId: string) {
    return request(`/api/checkout-lab/bookings/${bookingId}`, fulfillmentSchema)
  }

  return {
    createSession,
    createBooking,
    getHostedSession,
    simulate,
    getHostedFulfillment,
    listEvents,
    listDeliveries,
    listAnomalies,
    getBooking,
  }
}

export type { BookingResult }

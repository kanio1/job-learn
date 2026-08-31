import type { APIRequestContext, APIResponse } from '@playwright/test'
import {
  decodeResponse,
  type EmptyResult,
  type JsonResponseSchemas,
  type JsonResult,
  type ResponseSchemas,
  type BinaryResult,
  type TextResult,
} from './contracts/http-result'

export class BffTransport {
  constructor(private readonly context: APIRequestContext) {}

  async requestJson<TSuccess, TError>(
    endpoint: string,
    request: () => Promise<APIResponse>,
    schemas: JsonResponseSchemas<TSuccess, TError>,
  ): Promise<JsonResult<TSuccess, TError>>
  async requestJson<TSuccess, TError>(
    endpoint: string,
    request: () => Promise<APIResponse>,
    schemas: ResponseSchemas<TSuccess, TError>,
  ): Promise<JsonResult<TSuccess, TError> | EmptyResult>
  async requestJson<TSuccess, TError>(
    endpoint: string,
    request: () => Promise<APIResponse>,
    schemas: JsonResponseSchemas<TSuccess, TError> | ResponseSchemas<TSuccess, TError>,
  ): Promise<JsonResult<TSuccess, TError> | EmptyResult> {
    const response = await request()
    const input = {
      endpoint,
      status: response.status(),
      text: await response.text(),
      headers: response.headers(),
    }
    if ('empty' in schemas && schemas.empty !== undefined) {
      return decodeResponse(input, schemas)
    }
    return decodeResponse(input, schemas)
  }

  async dispose(): Promise<void> {
    await this.context.dispose()
  }

  async requestText(endpoint: string, request: () => Promise<APIResponse>): Promise<TextResult> {
    const response = await request()
    return {
      kind: 'text',
      status: response.status(),
      body: await response.text(),
      headers: response.headers(),
    }
  }

  async requestBinary(endpoint: string, request: () => Promise<APIResponse>): Promise<BinaryResult> {
    const response = await request()
    return {
      kind: 'binary',
      status: response.status(),
      body: Buffer.from(await response.body()),
      headers: response.headers(),
    }
  }
}

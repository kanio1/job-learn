import { describe, expect, it } from 'vitest'
import { z } from 'zod'
import { problemDetailsSchema } from '../utils/problem'
import { decodeResponse, expectError, expectSuccess } from './contracts/http-result'

const successSchema = z.object({ merchantId: z.string() })
const schemas = {
  success: { statuses: [200, 201], schema: successSchema },
  error: { statuses: [400, 401, 403, 404, 409, 422], schema: problemDetailsSchema },
  empty: { statuses: [204, 304] },
} as const

function decode(status: number, text: string) {
  return decodeResponse({
    endpoint: 'POST /api/merchants',
    status,
    text,
    headers: {},
  }, schemas)
}

describe('BFF JSON boundary characterization', () => {
  it('parses a valid success JSON body', () => {
    const result = expectSuccess(decode(201, '{"merchantId":"merchant-1"}'), 201)
    expect(result.body).toEqual({ merchantId: 'merchant-1' })
  })

  it('parses a valid problem JSON body when that is explicitly allowed', () => {
    const result = expectError(decode(400, '{"status":400,"title":"Bad request"}'), 400)
    expect(result.body).toMatchObject({ status: 400, title: 'Bad request' })
  })

  it.each([204, 304])('models an empty %s response explicitly', status => {
    expect(decode(status, '')).toMatchObject({ kind: 'empty', status })
  })

  it('rejects malformed JSON', () => {
    expect(() => decode(201, '{')).toThrow('malformed success JSON')
  })

  it('rejects a JSON body that does not satisfy the success schema', () => {
    expect(() => decode(201, '{"merchantReference":"M-1"}')).toThrow('invalid success JSON')
  })

  it('does not allow a 201 response to validate as a problem body', () => {
    expect(() =>
      decode(201, '{"status":400,"title":"Bad request"}'),
    ).toThrow('invalid success JSON')
  })
})

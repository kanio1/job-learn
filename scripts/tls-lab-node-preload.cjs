'use strict'
// Map TLS lab hostnames to loopback for the host Nuxt process (no /etc/hosts).
const dns = require('node:dns')

function rewrite(hostname) {
  if (hostname === 'auth.payment-quality.local'
    || hostname === 'app.payment-quality.local'
    || hostname === 'api.payment-quality.local') {
    return '127.0.0.1'
  }
  return hostname
}

const origLookup = dns.lookup
dns.lookup = function lookup(hostname, options, callback) {
  hostname = rewrite(hostname)
  if (typeof options === 'function') {
    return origLookup(hostname, options)
  }
  return origLookup(hostname, options, callback)
}

const origPromisesLookup = dns.promises.lookup.bind(dns.promises)
dns.promises.lookup = function lookup(hostname, options) {
  return origPromisesLookup(rewrite(hostname), options)
}

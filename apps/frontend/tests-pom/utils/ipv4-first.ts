import { setDefaultResultOrder } from 'node:dns'

/** Node (setup + BffClient) must not resolve `localhost` to ::1 while Nuxt binds 127.0.0.1. */
setDefaultResultOrder('ipv4first')

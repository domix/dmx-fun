// @ts-check
import { defineConfig } from 'astro/config';

import expressiveCode from "astro-expressive-code";

// https://astro.build/config
export default defineConfig({
  base: "/dmx-fun/",
  integrations: [expressiveCode()],
});
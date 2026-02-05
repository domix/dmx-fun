// 1. Import utilities from `astro:content`
import { defineCollection } from 'astro:content';

// 2. Import loader(s)
import { glob, file } from 'astro/loaders';

// 3. Import Zod
import { z } from 'astro/zod';

// 4. Define your collection(s)
const code = defineCollection({ /* ... */ });

// 5. Export a single `collections` object to register your collection(s)
export const collections = { code };

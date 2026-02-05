// 1. Import utilities from `astro:content`
import {defineCollection} from 'astro:content';

// 2. Import loader(s)

// 3. Import Zod

// 4. Define your collection(s)
const code = defineCollection({ /* ... */});

// 5. Export a single `collections` object to register your collection(s)
export const collections = {code};

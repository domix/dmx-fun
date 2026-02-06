import {defineCollection} from 'astro:content';
import {glob} from 'astro/loaders';
import {z} from 'astro/zod';


// Define your collection(s)
const code = defineCollection({
    loader: glob({pattern: "**/*.{md,mdx}", base: "./src/data/code"}),
    schema: z.object({
        fileName: z.string()
    })
});


// Export a single `collections` object to register your collection(s)
export const collections = {code};

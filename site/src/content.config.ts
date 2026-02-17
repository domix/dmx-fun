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

const blog = defineCollection({
    loader: glob({pattern: "**/*.{md,mdx}", base: "./src/data/blog"}),
    schema: z.object({
        title: z.string(),
        description: z.string(),
        pubDate: z.date(),
        author: z.string().default('dmx-fun Team'),
        authorImage: z.string().url().optional(),
        category: z.enum(['Tutorial', 'Best Practices', 'Release', 'Community', 'Guide', 'Article']),
        tags: z.array(z.string()).default([]),
        image: z.string().url().optional(),
        imageCredit: z.object({
            author: z.string(),
            authorUrl: z.string().optional(),
            source: z.string().optional(),
            sourceUrl: z.string().optional(),
        }).optional(),
    }),
});

// Export a single `collections` object to register your collection(s)
export const collections = {code, blog};

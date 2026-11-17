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
        category: z.enum(['Tutorial', 'Best Practices', 'Release', 'Community', 'Guide', 'Article', 'Fundamentals']),
        tags: z.array(z.string()).default([]),
        image: z.string().url().optional(),
        imageCredit: z.object({
            author: z.string(),
            authorUrl: z.string().url().optional(),
            source: z.string().optional(),
            sourceUrl: z.string().url().optional(),
        }).optional(),
    }),
});

const guide = defineCollection({
    loader: glob({pattern: "**/*.{md,mdx}", base: "./src/data/guide"}),
    schema: z.object({
        title: z.string(),
        description: z.string(),
        order: z.number(),
        h1: z.string(),
    }),
});

const adr = defineCollection({
    loader: glob({pattern: "**/*.md", base: "../docs/adr"}),
    schema: z.object({
        number: z.number().int().positive(),
        title: z.string(),
        status: z.enum(['Proposed', 'Accepted', 'Superseded']),
        date: z.date(),
    }),
});

const talks = defineCollection({
    loader: glob({pattern: "**/*.{md,mdx}", base: "./src/data/talks"}),
    schema: z.object({
        title: z.string(),
        description: z.string(),
        duration: z.number().int().positive(),            // minutes, questions included
        languages: z.array(z.enum(['en', 'es'])).nonempty(),
        status: z.enum(['upcoming', 'delivered']).default('upcoming'),
        deck: z.string(),                                  // path under the site base, e.g. talks/<slug>/
        pdf: z.object({en: z.string().optional(), es: z.string().optional()}).default({}),
        video: z.string().url().optional(),
        source: z.string().url(),                          // the deck's directory on GitHub
        related: z.array(z.object({label: z.string(), href: z.string()})).default([]),
        events: z.array(z.object({
            name: z.string(),
            date: z.date(),
            city: z.string().optional(),
            url: z.string().url().optional(),
        })).default([]),
    }),
});

// Export a single `collections` object to register your collection(s)
export const collections = {code, blog, guide, adr, talks};

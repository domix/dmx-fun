import rss from '@astrojs/rss';
import { getCollection } from 'astro:content';
import type { APIContext } from 'astro';
const base = import.meta.env.BASE_URL;

export async function GET(context: APIContext) {
    const posts = await getCollection('blog');
    const sortedPosts = posts.sort((a, b) => b.data.pubDate.getTime() - a.data.pubDate.getTime());

    return rss({
        title: 'dmx-fun Blog',
        description: 'Articles, tutorials, and insights about functional programming in Java',
        site: context.site || 'https://domix.github.io/dmx-fun/',
        items: sortedPosts.map((post) => ({
            title: post.data.title,
            pubDate: post.data.pubDate,
            description: post.data.description,
            author: post.data.author,
            link: `${base}blog/${post.id}/`,
            categories: [post.data.category, ...post.data.tags],
            customData: post.data.image
                ? `<enclosure url="${post.data.image}" type="image/jpeg" />`
                : undefined,
        })),
        customData: `<language>en-us</language>`,
    });
}

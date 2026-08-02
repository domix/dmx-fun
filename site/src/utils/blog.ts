export const PAGE_SIZE = 9;
// Page 1 of the blog has a hero post that consumes one extra slot,
// so it fetches PAGE_SIZE + 1 to keep the card grid at PAGE_SIZE (divisible by 3).
export const FIRST_PAGE_SIZE = PAGE_SIZE + 1;

export function normalizeCategorySlug(category: string): string {
  return category.toLowerCase().replaceAll(' ', '-');
}

/** Post count per category, for the counters shown next to category names. */
export function countByCategory(posts: { data: { category: string } }[]): Map<string, number> {
  const counts = new Map<string, number>();
  for (const post of posts) {
    counts.set(post.data.category, (counts.get(post.data.category) ?? 0) + 1);
  }
  return counts;
}

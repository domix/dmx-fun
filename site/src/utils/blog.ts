export const PAGE_SIZE = 9;
// Page 1 of the blog has a hero post that consumes one extra slot,
// so it fetches PAGE_SIZE + 1 to keep the card grid at PAGE_SIZE (divisible by 3).
export const FIRST_PAGE_SIZE = PAGE_SIZE + 1;

export function normalizeCategorySlug(category: string): string {
  return category.toLowerCase().replaceAll(' ', '-');
}

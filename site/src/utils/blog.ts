export const PAGE_SIZE = 9;

export function normalizeCategorySlug(category: string): string {
  return category.toLowerCase().replaceAll(' ', '-');
}

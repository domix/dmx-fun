export function calculateReadingTime(content: string): number {
    const wordsPerMinute = 200;
    const wordCount = content.trim().split(/\s+/).length;
    return Math.ceil(wordCount / wordsPerMinute);
}

export function formatReadingTime(minutes: number): string {
    return minutes === 1 ? '1 min read' : `${minutes} min read`;
}

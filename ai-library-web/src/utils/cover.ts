/** 封面兜底图（由 scripts/generate-covers.py 生成到 public/covers/） */
export const DEFAULT_COVER = '/covers/default.svg'

/** 封面来源：图书实体与借阅记录视图都只用到 isbn + cover 两个字段 */
export interface CoverSource {
  isbn?: string | null
  cover?: string | null
}

/** 按 ISBN 拼出内置封面路径，规则需与 scripts/generate-covers.py 保持一致 */
export function coverByIsbn(isbn?: string | null): string {
  const value = isbn?.trim()
  return value ? `/covers/${value}.svg` : DEFAULT_COVER
}

/**
 * 解析书籍封面地址。
 * 优先使用数据库 cover 字段（管理员上传的图片 / 外部 URL），
 * 为空时按 ISBN 回退到内置封面图，最后兜底 default.svg。
 */
export function resolveCover(book?: CoverSource | null): string {
  if (!book) return DEFAULT_COVER
  const cover = book.cover?.trim()
  return cover || coverByIsbn(book.isbn)
}

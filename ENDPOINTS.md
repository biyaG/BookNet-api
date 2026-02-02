# BookNet API Endpoints

Base path: `/api` (configured in `WebConfig` for all `@RestController` classes).

Auth header (when required): `Authorization: Bearer <JWT>`

## Authentication

- `POST /api/auth/register/reader` — Register a new reader. Auth: Public. Body: `ReaderRegistrationRequest`.
- `POST /api/auth/register/admin` — Register a new admin. Auth: Public. Body: `AdminRegistrationRequest`.
- `POST /api/auth/login` — Login, returns JWT in `Authorization` response header. Auth: Public. Body: `UserLoginRequest`.
- `POST /api/auth/login-alt` — Alternate login, returns JWT in `Authorization` response header. Auth: Public. Body: `UserLoginRequest`.
- `POST /api/auth/refresh` — Refresh JWT, returns new token in `Authorization` header. Auth: Required.
- `GET /api/auth/me` — Get token data. Auth: Required.

## Book

- `POST /api/book/upload/{idSource}` — Import books (NDJSON). Auth: Admin. Multipart: `file`.
- `POST /api/book/upload/similarity/{idSource}` — Import book similarity (NDJSON). Auth: Admin. Multipart: `file`.
- `POST /api/book/upload/genre/{idSource}` — Import book genre (NDJSON). Auth: Admin. Multipart: `file`.
- `GET /api/book/migrate` — Migrate books MongoDB -> Neo4j. Auth: Admin.
- `GET /api/book/{idBook}` — Get book by id. Auth: Public.
- `GET /api/book/{idBook}/reviews` — List reviews for a book. Auth: Public. Query: `page`, `size`.
- `POST /api/book/{idBook}/reviews` — Add review to a book. Auth: Reader. Body: `ReviewCreateRequest`.
- `POST /api/book/{idBook}/shelf` — Add book to current reader shelf. Auth: Reader.
- `PUT /api/book/{idBook}/shelf` — Update book shelf status. Auth: Reader. Body: `ReaderBookShelfUpdateStatusRequest`.
- `DELETE /api/book/{idBook}/shelf` — Remove book from current reader shelf. Auth: Reader.
- `DELETE /api/book/{idBook}` — Delete book. Auth: Admin.
- `POST /api/book/delete` — Delete multiple books. Auth: Admin. Body: list of `ObjectId`.
- `POST /api/book` — Create book. Auth: Admin. Body: `BookCreateRequest`.
- `GET /api/book` — List/search books. Auth: Public. Query: `page`, `size`, `name` (search by title when provided).
- `GET /api/book/by/genre/{idGenre}` — List books by genre. Auth: Public. Query: `page`, `size`.
- `GET /api/book/{idBook}/analytic/chart` — Analytics chart for a book. Auth: Admin. Query: `startDate`, `endDate` (yyyy-MM-dd), `granularity`.
- `GET /api/book/random` — Random books. Auth: Public. Query: `size`.
- `GET /api/book/popular/rating` — Popular books by rating. Auth: Public. Query: `size`, `dayAgo`.
- `GET /api/book/popular/shelf` — Popular books by shelf. Auth: Public. Query: `size`.
- `GET /api/book/recommendation` — Recommended books for current reader. Auth: Reader. Query: `size`.

## Author

- `POST /api/author/upload/{idSource}` — Import authors (NDJSON). Auth: Admin. Multipart: `file`.
- `GET /api/author/migrate` — Migrate authors MongoDB -> Neo4j. Auth: Admin.
- `GET /api/author/{idAuthor}` — Get author by id. Auth: Public.
- `DELETE /api/author/{idAuthor}` — Delete author. Auth: Admin.
- `POST /api/author/delete` — Delete multiple authors. Auth: Admin. Body: list of ids.
- `POST /api/author` — Create author. Auth: Admin. Body: `AuthorCreateRequest`.
- `GET /api/author` — List/search authors. Auth: Public. Query: `page`, `size`, `name` (search by name when provided).
- `GET /api/author/{idAuthor}/books` — List books by author. Auth: Public.
- `GET /api/author/{idAuthor}/analytic/chart` — Analytics chart for an author. Auth: Admin. Query: `startDate`, `endDate` (yyyy-MM-dd), `granularity`.
- `GET /api/author/most/written-books` — Top authors by written books. Auth: Public. Query: `size`.
- `GET /api/author/most/followed` — Top followed authors. Auth: Public. Query: `size`.
- `GET /api/author/most/read` — Top read authors. Auth: Public. Query: `size`.

## Genre

- `POST /api/genre/upload/{idSource}` — Import genres (NDJSON). Auth: Admin. Multipart: `file`.
- `GET /api/genre/migrate` — Migrate genres MongoDB -> Neo4j. Auth: Admin.
- `GET /api/genre/{idGenre}` — Get genre by id. Auth: Public.
- `DELETE /api/genre/{idGenre}` — Delete genre. Auth: Admin.
- `POST /api/genre/delete` — Delete multiple genres. Auth: Admin. Body: list of ids.
- `POST /api/genre` — Create genre. Auth: Admin. Body: `GenreCreateRequest`.
- `GET /api/genre` — List/search genres. Auth: Public. Query: `page`, `size`, `name` (search by name when provided).
- `GET /api/genre/{idGenre}/analytic/chart` — Analytics chart for a genre. Auth: Admin. Query: `startDate`, `endDate` (yyyy-MM-dd), `granularity`.

## Review

- `POST /api/review/upload/{idSource}` — Import reviews (NDJSON). Auth: Admin. Multipart: `file`.
- `GET /api/review/migrate` — Migrate reviews MongoDB -> Neo4j. Auth: Admin.
- `GET /api/review/{idReview}` — Get review by id. Auth: Public.
- `POST /api/review/{idReview}` — Update review. Auth: Reader. Body: `ReviewUpdateRequest`.
- `DELETE /api/review/{idReview}` — Delete review. Auth: Required (Admin can delete any; non-admin path is gated by controller checks).
- `POST /api/review/delete` — Delete multiple reviews. Auth: Admin. Body: list of ids.
- `GET /api/review` — List all reviews. Auth: Admin. Query: `page`, `size`.

## User

- `POST /api/user/reviewer/reads/upload/{idSource}` — Import reviewer reads (NDJSON). Auth: Admin. Multipart: `file`.
- `GET /api/user/migrate` — Migrate all users MongoDB -> Neo4j. Auth: Admin.
- `GET /api/user/migrate/reader` — Migrate readers MongoDB -> Neo4j. Auth: Admin.
- `GET /api/user/migrate/reviewer` — Migrate reviewers MongoDB -> Neo4j. Auth: Admin.
- `GET /api/user/admin` — List admins. Auth: Admin. Query: `page`, `size`.
- `GET /api/user/reader` — List readers. Auth: Admin. Query: `page`, `size`.
- `GET /api/user/reviewer` — List reviewers. Auth: Admin. Query: `page`, `size`.
- `GET /api/user/me` — Get current user profile. Auth: Required.
- `POST /api/user` — Update current user name. Auth: Required. Body: `UserUpdateRequest`.
- `POST /api/user/preference` — Update current reader preference. Auth: Reader. Body: `ReaderPreferenceRequest`.
- `GET /api/user/{idUser}` — Get user by id. Auth: Public.
- `GET /api/user/me/shelf` — Get current reader shelf. Auth: Reader.
- `GET /api/user/reader/{idUser}` — Get reader (complex) by id. Auth: Public.
- `GET /api/user/{idUser}/reviews` — Get user reviews. Auth: Public. Query: `page`, `size`.
- `GET /api/user/stat/monthly` — Get current reader monthly stat. Auth: Reader. Query: `year`, `month`.
- `GET /api/user/stat/monthly/list` — Get current reader monthly stats for a year. Auth: Reader. Query: `year`.
- `GET /api/user/stat/yearly` — Get current reader yearly stat. Auth: Reader. Query: `year`.

## Source

- `GET /api/source` — List sources. Auth: Public.
- `POST /api/source` — Add new source. Auth: Admin. Body: `SourceCreateRequest`.
- `GET /api/source/{idSource}` — Get source by id. Auth: Public.
- `DELETE /api/source/{idSource}` — Delete source. Auth: Admin.

## Notification

- `GET /api/notification` — List notifications (admin). Auth: Admin. Query: `page`, `size`, `read`.
- `GET /api/notification/latest` — Latest notifications (admin). Auth: Admin.
- `GET /api/notification/{idNotification}` — Get notification by id (admin). Auth: Admin.
- `DELETE /api/notification/{idNotification}` — Delete notification (admin). Auth: Admin.
- `POST /api/notification/delete` — Delete multiple notifications (admin). Auth: Admin. Body: list of ids.

## Notes

- All paths are prefixed by `/api` due to `WebConfig`.
- Upload endpoints consume `multipart/form-data` with parameter `file`.
- Pagination defaults are set in controllers (commonly `page=0`, `size=10` or `size=100` for user lists).

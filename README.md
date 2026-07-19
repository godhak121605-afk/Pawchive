# Pawchive

Pawchive is a Kotlin 2.x, Tachimanga/Tachiyomi-compatible extension that talks directly to the public Pawchive API. It has no backend, proxy, account credential, or hosted service.

## Features

- Latest posts via `GET /api/v1/posts?o=<offset>`.
- Post search via `GET /api/v1/posts?q=<query>&o=<offset>`.
- Creator browsing and client-side Popular sorting from schema-provided `favorited` counts in `GET /api/v1/creators`.
- Creator details and paged chapter lists via `GET /api/v1/{service}/user/{creator_id}/profile` and `GET /api/v1/{service}/user/{creator_id}?o=<offset>`.
- Post details and gallery pages via `GET /api/v1/{service}/user/{creator_id}/post/{post_id}`.
- OkHttp cache-control for post, detail, and creator responses; friendly timeout, network, rate-limit, and API errors.

`SManga` represents either a creator or a discovered post, `SChapter` represents a post, and `Page` represents each image attachment. The source follows the API's 50-item offset stepping exactly.

Pawchive API post payloads expose generic files as `name` plus `path`, not a video/player model. The compatible reader can only render images, so image attachments use direct `file.pawchive.pw` URLs and normal chapter downloads. Videos and archives stay available from the source post opened in Pawchive; they are intentionally not added as broken image pages. The media host mapping is derived from Pawchive's own post rendering, not scraped at runtime.

## Build

Use Java 17 and Android SDK platform 36, then run:

```sh
./gradlew test assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Signed releases

The release workflow runs for a `v*` tag or manually. Before the first release, configure these GitHub Actions secrets with one stable Android keystore:

- `PAWCHIVE_KEYSTORE_BASE64`
- `PAWCHIVE_KEYSTORE_PASSWORD`
- `PAWCHIVE_KEY_ALIAS`
- `PAWCHIVE_KEY_PASSWORD`

The keystore is intentionally ignored by Git. Keep an offline backup; losing it prevents future releases from updating an installed APK.

Each release publishes a standard Tachimanga/Mihon repository branch. Add this URL in Tachimanga to install Pawchive and receive later updates from the same source:

```
https://raw.githubusercontent.com/godhak121605-afk/Pawchive/repo/index.min.json
```

## License

MIT. Pawchive is unaffiliated with Pawchive and its content providers.

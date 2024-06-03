<h1 align="center">Battarang Notifier for Android</h1>

<div align="center">
  <a href="https://github.com/ni554n/battarang-notifier-android/releases"><img alt="GitHub Downloads" src="https://img.shields.io/github/downloads/ni554n/battarang-notifier-android/total?style=flat&logo=github&labelColor=%23031919&color=%2391d7b3"></a>
  <a href="https://play.google.com/store/apps/details?id=com.anissan.battarang"><img  alt="GitHub Downloads" src="https://img.shields.io/endpoint?color=%2391d7b3&logo=google-play&labelColor=%23031919&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcom.anissan.battarang%26gl%3DUS%26hl%3Den%26l%3Dplay%2520store%26m%3D%24installs"></a>
</div>

<br>

<p align="center">🔔🔋 Sync battery notifications across devices 🪫🔔</p>

![Battarang Features](.docs/features.png)

## Setup

1. **Download** the latest APK from [GitHub Releases](https://github.com/ni554n/battarang-notifier-android/releases) or consider purchasing it from the [Play Store](https://play.google.com/store/apps/details?id=com.anissan.battarang) for automatic updates and to support development.
2. **Install** the app on your sender device (or multiple devices).
3. To **Pair it with a receiver** device or Telegram, visit [battarang.anissan.com](https://battarang.anissan.com) from another device and follow the instructions.

> [!TIP]
>
> Sam Beckman created an awesome review and setup tutorial for Battarang on [YouTube](https://www.youtube.com/watch?v=xthkvsnNb-8&t=237s)
>
> <a href="https://www.youtube.com/watch?v=xthkvsnNb-8&t=237s"><img src=".docs/sam_beckman_review_thumbnail.jpg" width="300"></a>

## Architecture

The architecture of this project may seem unfamiliar to a seasoned Android developer
because I came up with an architecture that is tailored to the features of this project.
I took a web dev approach rather than over-engineering a Google scale solution where it feels
counter-productive.

Essentially the source of truth is SharedPref KV storage and, the Views get updated by observing the
changes.
It's kind of like a poor man's reactive system.

I've also heavily used Kotlin Extension Functions rather than Classes to help with the composition
to minimize the changes to a few places as possible.

## Build

1. Copy the properties from [local.example.properties](local.example.properties)
   to `local.properties` and provide the values
2. `🔨 Make Project` or `▶️ Run` the app

## Information

**Author:** [Nissan Ahmed](https://anissan.com) ([@ni554n](https://twitter.com/ni554n))

**Donate:** [PayPal](https://paypal.me/ni554n)
<img src="https://ping.anissan.com/?repo=battarang-notifier-android" width="0" height="0" align="right">

## License

This project intentionally
has [no license](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository#choosing-the-right-license)
so the default copyright laws apply,
which means I retain all rights to the source code and the graphical assets.
No one may reproduce, distribute, or create derivative works from this work without my permission.

However you are free to view, contribute, and copy parts of the code into your own project without
attribution.
You are just not allowed to repackage and redistribute the entire app.

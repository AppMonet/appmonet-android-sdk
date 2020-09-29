<br />
<p align="center">
  <a href="https://appmonet.com/">
    <img src="images/appmonet.png" alt="Logo" >
  </a>

  <h3 align="center">AppMonet Android SDK</h3>

  <p align="center">
    Add header bidding and additional demand partners<br />in banner, interstitial, and native ads!
    <br />
    <a href="https://docs.appmonet.com/docs/get-started-with-appmonet"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/othneildrew/Best-README-Template/issues">Report Bug</a>
    ·
    <a href="https://github.com/othneildrew/Best-README-Template/issues">Request Feature</a>
  </p>
</p>

## Table of Contents

* [About the Project](#about-the-project)
* [Getting Started](#getting-started)
  * [Installation](#installation)
    * [Standalone Bidder Setup](#standalone-bidder-setup)
* [Contributing](#contributing)
* [License](#license)

## About The Project


![Architecture][architecture-screenshot]


This repository contains the code for the [AppMonet android SDK](http://appmonet.com/). This SDK lets app developers add header bidding and additional demand partners in banner, interstitial, and native ads. There are currently 3 variants of the SDK:

- MoPub: integration into the MoPub adserver & SDK. Allows AppMonet to bid into MoPub to provide header-bidding competition.
- GAM/AdMob: header bidding and mediation integrations with GAM and AdMob.
- Standalone/"Bidder": for use with custom adservers/mediation SDKs, or with AppMonet as the primary adserver.

For more information on integrating the AppMonet SDK into your app, please see the [AppMonet Docs](https://docs.appmonet.com/docs)

## Getting Started

To get a local copy up and running follow these simple example steps.

### Installation

1. Clone the repo
```sh
git clone https://github.com/AppMonet/android-sdk.git
```
2. Gradle Sync
3. Choose a flavor
    - admob
    - dfp
    - mopub
    - appmonet (please refer to [Standalone Bidder Setup](#standalone-bidder-setup))
4. Build

#### Standalone Bidder Setup
In order to be able to build the appmonet standalone sample app flavor, you will need to set two values in your local.properties file.
```properties
standalone.enabled=true
appmonet.apiKey=api_key_from_appmonet_account
```

## Contributing

Contributions are welcomed! Feel free to offer suggestions and fixes to make our SDK better!

1. Fork the Project
2. Create your Feature or Fix Branch (`git checkout -b feature/branch_name` / `git checkout -b fix/branch_name`)
3. Commit your Changes (`git commit -m 'Add some awesome code'`)
4. Push to the Branch (`git push origin feature/branch_name`/ `git push origin fix/branch_name`)
5. Make sure all checks pass.
6. Open a Pull Request

## License
For full license, vist [https://appmonet.com/legal/sdk-license/](https://appmonet.com/legal/sdk-license/).


[architecture-screenshot]: images/architecture.png


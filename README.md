# cordova-yandex-sdk

1. Add the plugin<br>
```properties
cordova plugin add https://github.com/danillo19/cordova-yandex-sdk.git
```

2. Add calls to the plugin to load yandex ads<br>
```js
adMob.banner("yandex-ad-unit-id").catch(function(err){});
```

Code for testing methods:
```js
document.addEventListener('deviceready', onDeviceReady, false);
document.addEventListener("resume", onResume, false);

async function runAllOfTheAds(){
    await yaAdMob.banner("demo-banner-yandex").then(function(){
        alert("loaded banner ads");
    }).catch(function(err){
        alert("unable to load ads: "+JSON.stringify(err));
    });
    
    await yaAdMob.interstitial("demo-interstitial-yandex").then(function(){
        alert("loaded interstitial ads");;
        return yaAdMob.showInterstitial();
    }).then(function(){
        alert("showed interstitial ads");
    }).catch(function(err){
        alert("unable to load ads: "+JSON.stringify(err));
    });
    
    await yaAdMob.rewarded("demo-rewarded-yandex").then(function(){
        console.log("loaded rewarded ads");
        return yaAdMob.showRewarded();
    }).then(function(reward){
        alert("showed rewarded ads"+JSON.stringify(reward));
    }).catch(function(err){
        alert("unable to load rewarded ads: "+JSON.stringify(err));
    });

}

function onDeviceReady() {
    runAllOfTheAds();
}

async function showAppOpenAd() {
    await yaAdMob.loadAppOpenAd("demo-appopenad-yandex").then(()=> {
        alert("loaded app open ad");
        return yaAdMob.showAppOpenAd();
    });
}

function onResume() {
    showAppOpenAd();
}
```
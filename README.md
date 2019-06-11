# Android-Running-Parameters

<p>Android Mobile app that allow to monitor running parameters live. Control yout cadence, pace and step length.<br>
Based on researches available I was able to create formula for most energy efficient cadence.<br>
Formula: y = 215 – 1/6x. (±7 range). X - time in seconds of one kilometer pace.<br>
ex. 4:00min/km y= 215 - 1/6 * 240 = 175.  —&gt; 168-182 cadence should be optimal in most cases.</p>
<p>During training app check if cadence is optimal or not and show proper visualization to user.</p>
<p>App uses fusedLocationProvider and built-in step counter sensor</p>
<p>This app was part of my bachelor thesis project. I didn’t upload database configuration and traning save option because it’s less important part in this project so I didn’t focus on it. The are probably much better database implementations on github.</p>
<img src="https://user-images.githubusercontent.com/47602711/59259817-bdb10e80-8c3a-11e9-8dc6-ef76b8a616b4.png" alt="screen1"></p>
<p>*You need to enter correct Google Maps API key to get map view</p>

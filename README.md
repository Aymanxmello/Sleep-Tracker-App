# 🌙 Sleep Tracker App

<div align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Firebase-Enabled-orange.svg?style=for-the-badge&logo=firebase" />
  <img src="https://img.shields.io/badge/Android-Platform-green.svg?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" />
</div>


---

## 📖 Table of Contents
- [✨ Features](#-features)
- [📱 App Screenshots](#-app-screenshots)
- [🛠️ Tech Stack](#-tech-stack)
- [🚀 Getting Started](#-getting-started)
- [📂 Project Structure](#-project-structure)
- [🤝 How to Contribute](#-how-to-contribute)
- [📫 Author](#-author)

---

## ✨ Features

| Feature | Description |
| :--- | :--- |
| **📊 Smart Dashboard** | Interactive charts with sleep stats & weather info. |
| **🛌 Auto Sleep Tracking** | Detects movement via sensors & calculates sleep quality. |
| **📝 Manual Logging** | Add sleep data manually with ratings. |
| **🎯 Goal Setting** | Track your sleep goals with visual progress bars. |
| **💡 Sleep Tips** | Curated articles and videos to improve your sleep hygiene. |
| **☁️ Cloud Sync** | Firebase Auth & Firestore keep your data safe and synced. |
| **🎨 Dynamic UI** | Smooth animations, gradients, and starry night theme. |

---

## 📱 App Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Splash</b><br>Auto Sensor & Timer</td>
      <td align="center"><b>Dashboard</b><br>View Stats & Weather</td>
    </tr>
    <tr>
      <td><img src="screenshots/splash.jpeg" width="250" alt="Tracking Screen" /></td>
      <td><img src=".screenshots/dashboard.jpeg" width="250" alt="Dashboard Screen" /></td>
    </tr>
    <tr>
      <td align="center"><b>Goals</b><br>Set Targets & History</td>
      <td align="center"><b>Tips</b><br>Learn Better Habits</td>
    </tr>
    <tr>
      <td><img src="screenshots/goals.jpeg" width="250" alt="Goals Screen" /></td>
      <td><img src="screenshots/tips.jpeg" width="250" alt="Tips Screen" /></td>
    </tr>
  </table>
</div>


---

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/) (JVM Toolchain 21)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Backend:**
    * [Firebase Auth](https://firebase.google.com/docs/auth) (Login/Register)
    * [Firebase Firestore](https://firebase.google.com/docs/firestore) (NoSQL Database)
* **Network:** [Retrofit](https://square.github.io/retrofit/) & Gson
* **UI/UX:** MPAndroidChart, Lottie Animations, Material Components
* **Hardware:** Accelerometer sensors (`SensorManager`) & location (`FusedLocationProviderClient`)

---

> **Sleep better. Live better.**  
> Built with ❤️ by **Aymen Zemrani**.  
> A modern Android application built with **Kotlin** & **Firebase** to help track your sleep, improve your sleep hygiene, and wake up refreshed.

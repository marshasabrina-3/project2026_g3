# TaskGO UTM 🚀 (Version 2.5.0)

TaskGO is a modern, student-centric marketplace application designed for the UTM community. It connects students who need assistance with those offering specialized services, fostering a helpful and efficient campus ecosystem.

## 🌟 Key Features

### 👤 Advanced Public Profiles (New in v2.5.0)
- **Universal Profile View**: Users and Admins can view any student's detailed profile with full activity and review history.
- **Activity Transparency**: Profiles display posted tasks/services and completed work history as a runner (including payment amounts and requester attribution).
- **Comprehensive Reviews**: Integrated a rating and review system on profiles. Community members can write reviews, filter by star ratings, and sort by date (Newest/Oldest).
- **Trust Indicators**: High-visibility status badges (ACTIVE, SUSPENDED, BANNED) and centered user info for better readability.

### 🛡️ Enhanced Trust & Safety
- **Advanced User Reporting**: Categorized reporting system (Scam, Bullying, Fraud, etc.) with detailed audit logs for administrators.
- **Improved Navigation**: Seamless back-stack management ensures users return to the profile they were investigating, not the home screen.

### 📱 Responsive & Adaptive UI
- **Marketplace Clarity**: High-clarity unified feed with optimized item layouts to prevent information overlap between titles and prices.
- **Adaptive Nav Bar**: Optimized navigation height and system padding for consistent scaling on tablets and larger devices.
- **Modern Branding**: Transparent app logo integration for a cleaner campus aesthetic.

### 🗺️ Intelligent Real-Time Navigation
- **Map Selector**: Pin exact locations on a built-in map for precise coordination.
- **GPS Integration**: Navigate using external GPS apps directly from task details.
- **Campus-Aware Centering**: Automatically centers based on selected campus (UTMKL/UTMJB).

### 🛡️ Unified Admin Console
- **Direct User Investigation**: Jump from management lists to full public profiles.
- **Evidence Investigation**: Match runner completion proofs with requester payment receipts.
- **Bad Review Monitoring**: Dedicated dashboard for low-quality service reviews.

### 🔄 Integrated Task Lifecycle
- **Proof-Based Completion**: Integrated verification workflow for both runners and requesters.
- **Payment Verification**: Real-time status tracking (PAID, PENDING, DISPUTED).

## 🛠️ Technical Architecture

- **UI Framework**: Jetpack Compose (Adaptive Navigation & Back-Stack Management).
- **Backend**: Firebase Authentication, Firestore, and Storage.
- **Maps**: Google Maps SDK for Android.
- **Architecture**: MVVM with StateFlow & Persistent Navigation State.

---

*Developed for the Project 2026 UTM community.*

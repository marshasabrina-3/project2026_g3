# TaskGO 🚀

TaskGO is a modern, student-centric marketplace application designed for the UTM community. It connects students who need assistance with those offering specialized services, fostering a helpful and efficient campus ecosystem.

## 🌟 Key Features

### 🛒 Dynamic Marketplace
- **Explore Tasks**: A unified feed of "Open Requests" and "Service Offers".
- **Advanced Filtering**: Filter by category (Food, Carpool, Printing, etc.) and campus (UTMKL or UTMJB).
- **Smart Sorting**: Sort tasks by Newest First, Highest/Lowest Price, or Alphabetically.
- **Detailed View**: High-resolution image carousel, detailed descriptions, and direct access to poster profiles.

### 🎨 Creation Studio
- **Post Requests**: Need help with something? Post it here with a title, description, and price.
- **Offer Services**: Have a skill or a spare vehicle? Offer your services to the community.
- **Flexible Options**: Opt to include or omit specific deadlines and prices for service offers.
- **Attachment Support**: Upload up to 7 images per post to provide clear visual context.

### 💬 Advanced Chat System
- **Real-time Messaging**: Instant communication between Requesters and Runners powered by Firebase Firestore.
- **Task-Contextual Chats**: Every chat is anchored to a specific task, featuring a "Task Reference" card for clarity.
- **Unread Indicators**: Real-time notification badges on the main navigation and per-conversation unread counts.
- **Image Sharing**: Send images directly within the chat for verification or clarification.

### 🛡️ User Reputation & Safety
- **Public Profiles**: View user names, average star ratings, and total report counts.
- **Ratings & Reviews**: Rate your experience after task completion to maintain high community standards.
- **Reporting System**: Report issues or users directly within the app to ensure a safe environment.

## 🛠️ Technical Architecture

- **UI Framework**: Jetpack Compose (Modern, declarative UI).
- **Backend**: 
    - **Firebase Authentication**: Secure student sign-in.
    - **Firebase Firestore**: Real-time NoSQL database for tasks, chats, and user data.
- **Image Handling**: Optimized Base64 encoding/decoding system (stored in Firestore to minimize storage overhead).
- **Image Loading**: Coil (Async image loading with custom ByteArray support).
- **Architecture**: MVVM (Model-View-ViewModel) for clean separation of concerns.

## 🎨 Design Language
TaskGO follows a professional theme utilizing **UTM Maroon (#800000)** as the primary brand color, combined with a clean Material 3 design system for a premium user experience.

---

*Developed for the Project 2026 UTM community.*

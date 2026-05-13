# TaskGO UTM 🚀

TaskGO is a modern, student-centric marketplace application designed for the UTM community. It connects students who need assistance with those offering specialized services, fostering a helpful and efficient campus ecosystem.

## 🌟 Key Features

### 🛒 Dynamic Marketplace
- **Explore Tasks**: A unified feed of "Open Requests" and "Service Offers".
- **Advanced Filtering**: Filter by category (Food, Carpool, Printing, etc.) and campus (UTMKL or UTMJB).
- **Smart Sorting**: Sort tasks by Newest First, Highest/Lowest Price, or Alphabetically.
- **Detailed View**: High-resolution image carousel, detailed descriptions, timestamps (Posted/Completed), and direct access to poster profiles.

### 🎨 Creation Studio
- **Post Requests**: Need help with something? Post it here with a title, description, and price.
- **Offer Services**: Have a skill or a spare vehicle? Offer your services to the community.
- **Task Management**: Cancel active posts or withdraw applications directly from your history.
- **Categorized Activity**: Separate lists for Requests and Services to help you stay organized.
- **Flexible Options**: Opt to include or omit specific deadlines and prices for service offers.
- **Attachment Support**: Upload up to 7 images per post to provide clear visual context.

### 💬 Advanced Chat System
- **Real-time Messaging**: Instant communication between Requesters and Runners powered by Firebase Firestore.
- **Task-Contextual Chats**: Every chat is anchored to a specific task, featuring a "Task Reference" card for clarity.
- **Unread Indicators**: Real-time notification badges on the main navigation and per-conversation unread counts.
- **Actual Names**: Displays user names (e.g., ALI BIN ALI) instead of internal database IDs for a more human experience.
- **Image Sharing**: Send images directly within the chat for verification or clarification.

### 🛡️ User Reputation & Safety
- **Public Profiles**: View user names, average star ratings, and total report counts.
- **Global Ratings**: Average ratings are displayed next to user names everywhere (Home, Chat, Details).
- **Ratings & Reviews**: Functional rating system after task completion to maintain high community standards.
- **Reporting System**: Report runners or app issues directly within the app to ensure a safe environment.

## 🛠️ Technical Architecture

- **UI Framework**: Jetpack Compose (Modern, declarative UI).
- **Backend**: 
    - **Firebase Authentication**: Secure student sign-in.
    - **Firebase Firestore**: Real-time NoSQL database with optimized hierarchical storage for chats.
- **Image Handling**: Optimized Base64 encoding/decoding system (stored in Firestore to minimize storage overhead).
- **Architecture**: MVVM (Model-View-ViewModel) for clean separation of concerns.

## 🎨 Design Language
TaskGO follows a professional theme utilizing **UTM Maroon (#800000)** as the primary brand color, combined with a clean Material 3 design system and official UTM branding.

---

*Developed for the Project 2024 UTM community.*

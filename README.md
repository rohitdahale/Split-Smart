# SplitSmart — The Smart Expense Splitter App 💸🔬

Welcome to **SplitSmart**, your ultimate solution for tracking shared expenses with friends, family, or colleagues. With a clean UI and real-time updates, SplitSmart makes splitting expenses effortless, transparent, and fun! 🌟

---

## Features 🔧

### Basic Features ✨
- **🔐 User Authentication**
  - Email and Password Login.
  - Google Sign-In for quick access.
- **📄 Group Management**
  - Create groups with a title, description, and optional cover image.
  - Add or remove members via email or phone number.
- **📝 Expense Management**
  - Add expenses manually with title, amount, payer, and split details.
  - Attach receipt images.
  - View detailed breakdowns of each expense.
- **🔔 Notifications**
  - Notify members of new expenses or unpaid amounts.
- **📊 Firebase Integration**
  - Firebase Realtime Database for real-time updates.
  - Firebase Storage for uploading and retrieving receipt images.
- **🌌 User-Friendly UI**
  - Material Design principles with support for dark mode.

### Intermediate Features 🔝
- **✉ Payment Settlements**
  - Mark expenses as partially or fully paid.
  - Automatically update remaining balances.
  - Visually distinguish settled and outstanding expenses.
- **⚖️ Expense Categorization**
  - Organize expenses by categories like food, travel, or utilities.
- **🔍 Enhanced Search and Filter**
  - Quickly find expenses by title, category, or group.

### Advanced Features 🌟
- **🔄 OCR for Receipts**
  - Use Firebase ML Kit to extract text from receipt images.
- **🚀 Analytics Dashboard**
  - Visualize group expenses with charts and insights.
- **🏦 Multi-Currency Support**
  - Handle expenses in different currencies seamlessly.
- **🛎 Notifications with Push**
  - Push notifications to alert group members of updates.

---

## Tech Stack 🛠

- **Frontend:** Native Android (Kotlin, XML)
- **Backend:** Firebase Realtime Database, Firebase Storage
- **Libraries:**
  - Glide 🌈 (for image loading)
  - Firebase Auth and ML Kit 📱
  - RecyclerView (for lists)
- **Development Tools:**
  - Android Studio ⚙️
  - Firebase Console 🔧

---

## Getting Started 🌍

### Prerequisites 📊

Ensure you have the following installed:
- Android Studio (latest version)
- Firebase account with a configured project

### Installation Steps 🌄

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/SplitSmart.git
   ```

2. **Open in Android Studio:**
   - Open the project in Android Studio.
   - Sync the Gradle files.

3. **Set up Firebase:**
   - Add the `google-services.json` file to the `app/` directory.
   - Enable Firebase Authentication, Realtime Database, and Storage.

4. **Run the App:**
   - Connect an Android device or use an emulator.
   - Build and run the app!

---

## Screenshots 🖼️

| Feature          | Screenshot            |
|------------------|-----------------------|
| Home Screen      | ![Home Screen](link) |
| Group Details    | ![Group Details](link) |
| Add Expense      | ![Add Expense](link) |
| Expense Breakdown| ![Breakdown](link) |

---

## Contribution Guidelines 🧬

We welcome contributions to improve SplitSmart! Follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add your message here"
   ```
4. Push to your branch:
   ```bash
   git push origin feature/your-feature-name
   ```
5. Create a pull request.

---

## License 🔒

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Support 🙏

Have questions or suggestions? Feel free to:
- Open an issue on GitHub.
- Email us at `support@splitsmart.com`.

---

Thank you for using **SplitSmart**! Together, we make sharing expenses easy and smart. ✨

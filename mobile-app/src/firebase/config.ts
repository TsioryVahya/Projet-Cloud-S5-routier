import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getAuth } from 'firebase/auth';

// Configuration Firebase pour le module mobile
// Note: Dans un projet réel, ces valeurs devraient être dans un fichier .env
const firebaseConfig = {
  apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
  authDomain: "projet-cloud-s5-routier.firebaseapp.com",
  projectId: "projet-cloud-s5-routier",
  storageBucket: "projet-cloud-s5-routier.firebasestorage.app",
  messagingSenderId: "792049548362",
  appId: "1:792049548362:web:6ab3ce65b1584730c63ab3" // ID Web déduit (ou Android si Web non créé)
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);

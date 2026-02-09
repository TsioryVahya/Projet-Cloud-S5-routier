import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.projet.cloud.routier',
  appName: 'mobile-app',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  }
};

export default config;

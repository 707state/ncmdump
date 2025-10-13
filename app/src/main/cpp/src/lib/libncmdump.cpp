#include "libncmdump.h"

#include <jni.h>

#include <filesystem>

namespace fs = std::filesystem;

extern "C" {
API NeteaseCrypt *CreateNeteaseCrypt(const char *path) {
  fs::path fPath = fs::u8path(path);
  return new NeteaseCrypt(fPath.u8string());
}

API int Dump(NeteaseCrypt *neteaseCrypt, const char *outputPath) {
  try {
    neteaseCrypt->Dump(outputPath);
  } catch (const std::invalid_argument &e) {
    return 1;
  }
  return 0;
}

API void FixMetadata(NeteaseCrypt *neteaseCrypt) {
  neteaseCrypt->FixMetadata();
}

API void DestroyNeteaseCrypt(NeteaseCrypt *neteaseCrypt) {
  delete neteaseCrypt;
}
}
extern "C" JNIEXPORT jstring
Java_com_example_player2_MainActivity_stringFromJNI(JNIEnv *env,
                                                    jobject /* this */) {
  std::string hello = "Hello from C++";
  return env->NewStringUTF(hello.

                           c_str()

  );
}
extern "C" JNIEXPORT jint Java_com_netease_ncmdump_NcmBridge_convertAll(
    JNIEnv *env, jobject thiz, jstring jInputFolder, jstring jOutputFolder) {
  const char *inputFile = env->GetStringUTFChars(jInputFolder, nullptr);
  const char *outputFolder = env->GetStringUTFChars(jOutputFolder, nullptr);

  int converted = 0;

  try {
    // 直接处理传入的单个 NCM 文件
    auto ncm = CreateNeteaseCrypt(inputFile);
    Dump(ncm, outputFolder);
    FixMetadata(ncm);
    DestroyNeteaseCrypt(ncm);
    converted = 1; // 成功处理一个文件
  } catch (const std::exception &e) {
    std::cerr << "Caught exception: " << e.what() << std::endl;
  } catch (...) {
    std::cerr << "Caught unknown exception!" << std::endl;
  }

  env->ReleaseStringUTFChars(jInputFolder, inputFile);
  env->ReleaseStringUTFChars(jOutputFolder, outputFolder);
  return converted;
}
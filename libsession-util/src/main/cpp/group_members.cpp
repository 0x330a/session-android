#include "group_members.h"

extern "C"
JNIEXPORT jobject JNICALL
Java_network_loki_messenger_libsession_1util_GroupMembersConfig_00024Companion_newInstance(
        JNIEnv *env, jobject thiz, jbyteArray pub_key, jbyteArray secret_key,
        jbyteArray initial_dump) {
    std::lock_guard lock{util::util_mutex_};
    auto pub_key_bytes = util::ustring_from_bytes(env, pub_key);
    std::optional<session::ustring> secret_key_optional{std::nullopt};
    std::optional<session::ustring> initial_dump_optional{std::nullopt};
    if (env->GetArrayLength(secret_key) == 32 || env->GetArrayLength(secret_key) == 64) {
        auto secret_key_bytes = util::ustring_from_bytes(env, secret_key);
        secret_key_optional = secret_key_bytes;
    }
    if (env->GetArrayLength(initial_dump) > 0) {
        auto initial_dump_bytes = util::ustring_from_bytes(env, initial_dump);
        initial_dump_optional = initial_dump_bytes;
    }

    auto* group_members = new session::config::groups::Members(pub_key_bytes, secret_key_optional, initial_dump_optional);

    jclass groupMemberClass = env->FindClass("network/loki/messenger/libsession_util/GroupMembersConfig");
    jmethodID constructor = env->GetMethodID(groupMemberClass, "<init>", "(J)V");
    jobject newConfig = env->NewObject(groupMemberClass, constructor, reinterpret_cast<jlong>(group_members));

    return newConfig;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_network_loki_messenger_libsession_1util_GroupMembersConfig_all(JNIEnv *env, jobject thiz) {
    std::lock_guard lock{util::util_mutex_};
    auto config = ptrToMembers(env, thiz);
    jclass stack = env->FindClass("java/util/Stack");
    jmethodID init = env->GetMethodID(stack, "<init>", "()V");
    jobject our_stack = env->NewObject(stack, init);
    jmethodID push = env->GetMethodID(stack, "push", "(Ljava/lang/Object;)Ljava/lang/Object;");
    for (auto& member : *config) {
        auto member_obj = util::serialize_group_member(env, member);
        env->CallObjectMethod(our_stack, push, member_obj);
    }
    return our_stack;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_network_loki_messenger_libsession_1util_GroupMembersConfig_erase(JNIEnv *env, jobject thiz,
                                                                      jobject group_member) {
    auto config = ptrToMembers(env, thiz);
    auto member = util::deserialize_group_member(env, group_member);
    return config->erase(member.session_id);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_network_loki_messenger_libsession_1util_GroupMembersConfig_get(JNIEnv *env, jobject thiz,
                                                                    jstring pub_key_hex) {
    std::lock_guard lock{util::util_mutex_};
    auto config = ptrToMembers(env, thiz);
    auto pub_key_bytes = env->GetStringUTFChars(pub_key_hex, nullptr);
    auto member = config->get(pub_key_bytes);
    if (!member) {
        return nullptr;
    }
    auto serialized = util::serialize_group_member(env, *member);
    env->ReleaseStringUTFChars(pub_key_hex, pub_key_bytes);
    return serialized;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_network_loki_messenger_libsession_1util_GroupMembersConfig_getOrConstruct(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jstring pub_key_hex) {
    std::lock_guard lock{util::util_mutex_};
    auto config = ptrToMembers(env, thiz);
    auto pub_key_bytes = env->GetStringUTFChars(pub_key_hex, nullptr);
    auto member = config->get_or_construct(pub_key_bytes);
    auto serialized = util::serialize_group_member(env, member);
    env->ReleaseStringUTFChars(pub_key_hex, pub_key_bytes);
    return serialized;
}

extern "C"
JNIEXPORT void JNICALL
Java_network_loki_messenger_libsession_1util_GroupMembersConfig_set(JNIEnv *env, jobject thiz,
                                                                    jobject group_member) {
    std::lock_guard lock{util::util_mutex_};
    auto config = ptrToMembers(env, thiz);
    auto deserialized = util::deserialize_group_member(env, group_member);
    config->set(deserialized);
}
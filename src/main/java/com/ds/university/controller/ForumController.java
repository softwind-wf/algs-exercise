package com.ds.university.controller;

import com.ds.university.common.BusinessException;
import com.ds.university.common.PageResult;
import com.ds.university.entity.ForumPost;
import com.ds.university.service.ForumService;
import com.ds.university.vo.LoginUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/**
 * 学习论坛：所有学生/教师（及管理员）可发帖、回复、点赞；
 * 作者可编辑/删除自己的帖子与回复，管理员可置顶/加精/删除；
 * 公开浏览无需登录，写操作需登录。
 */
@Controller
@RequestMapping("/forum")
public class ForumController {

    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    /** 帖子列表（关键字搜索 + 分类筛选 + 分页） */
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       HttpSession session, Model model) {
        LoginUser me = currentUser(session);
        model.addAttribute("posts", forumService.page(keyword, category,
                page == null ? 1 : page, size == null ? 0 : size, me == null ? null : me.getUserId()));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selCategory", category == null ? "" : category);
        model.addAttribute("categories", forumService.categories());
        return "forum/list";
    }

    /** 发帖页（需登录） */
    @GetMapping("/new")
    public String newPost(HttpSession session, Model model) {
        if (currentUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("categories", forumService.categories());
        return "forum/new";
    }

    /** 提交发帖（需登录） */
    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(required = false) String category,
                         HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            forumService.createPost(user, title, content, category);
            ra.addFlashAttribute("success", "发帖成功");
            return "redirect:/forum";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("title", title);
            ra.addFlashAttribute("content", content);
            return "redirect:/forum/new";
        }
    }

    /** 编辑帖页（仅作者） */
    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, HttpSession session, Model model) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        ForumPost post = forumService.getPost(id, user.getUserId());
        if (!isAuthorOrAdmin(user, post)) {
            throw new BusinessException(com.ds.university.common.ErrorCode.UNAUTHORIZED, "只能编辑自己发布的帖子");
        }
        model.addAttribute("post", post);
        model.addAttribute("categories", forumService.categories());
        return "forum/edit";
    }

    /** 提交编辑（仅作者） */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(required = false) String category,
                         HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            forumService.updatePost(user, id, title, content, category);
            ra.addFlashAttribute("success", "帖子已更新");
            return "redirect:/forum/" + id;
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/forum/" + id + "/edit";
        }
    }

    /** 帖子详情（帖子 + 全部回复 + 回复表单 + 点赞/编辑/管理按钮） */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        LoginUser me = currentUser(session);
        model.addAttribute("post", forumService.getPost(id, me == null ? null : me.getUserId()));
        model.addAttribute("replies", forumService.replies(id));
        model.addAttribute("categories", forumService.categories());
        return "forum/detail";
    }

    /** 回复（需登录） */
    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id,
                        @RequestParam String content,
                        HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            forumService.createReply(user, id, content);
            ra.addFlashAttribute("success", "回复成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forum/" + id;
    }

    /** 点赞/取消点赞（登录用户，切换） */
    @PostMapping("/{id}/like")
    public String like(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            boolean liked = forumService.toggleLike(user, id);
            ra.addFlashAttribute("success", liked ? "点赞成功" : "已取消点赞");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forum/" + id;
    }

    /** 置顶/取消置顶（管理员） */
    @PostMapping("/{id}/pin")
    public String pin(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            boolean pinned = forumService.togglePinned(user, id);
            ra.addFlashAttribute("success", pinned ? "已置顶" : "已取消置顶");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forum/" + id;
    }

    /** 加精/取消加精（管理员） */
    @PostMapping("/{id}/feature")
    public String feature(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            boolean featured = forumService.toggleFeatured(user, id);
            ra.addFlashAttribute("success", featured ? "已加精" : "已取消加精");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forum/" + id;
    }

    /** 删除帖子（作者或管理员） */
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            forumService.deletePost(user, id);
            ra.addFlashAttribute("success", "帖子已删除");
            return "redirect:/forum";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/forum/" + id;
        }
    }

    /** 删除回复（回复作者或管理员） */
    @PostMapping("/reply/{replyId}/delete")
    public String deleteReply(@PathVariable Long replyId,
                              @RequestParam Long postId,
                              HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            forumService.deleteReply(user, replyId);
            ra.addFlashAttribute("success", "回复已删除");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forum/" + postId;
    }

    private LoginUser currentUser(HttpSession session) {
        return session == null ? null : (LoginUser) session.getAttribute(AuthController.SESSION_USER);
    }

    private boolean isAuthorOrAdmin(LoginUser user, ForumPost post) {
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ADMIN");
        return isAdmin || user.getUserId().equals(post.getAuthorUser());
    }
}
